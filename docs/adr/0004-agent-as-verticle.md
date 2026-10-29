# Agent as Vert.x verticle — event-driven agent runtime

## Status

Proposed (pending spike: thread-affinity audit + drain-task prototype).

## Context

JADE agents are long-lived virtual threads (`Thread.ofVirtual()` in
`ResourceManager`/`FullResourceManager`). Each agent runs `Agent.run()` —
a `while(alive)` loop that cooperatively schedules behaviours via
`Scheduler` (round-robin over ready behaviours, parks on `Object.wait`
when idle). `blockingReceive`/`doWait` park the agent's thread on the
`msgQueue` monitor. Message delivery is queued through 5 shared
`MessageManager` deliverer threads; the receiver is only woken
(`postMessage` → `doWake` → `notifyAll`).

Vert.x exists in the codebase but is entirely separate from agents: one
private `Vertx.vertx()` in `Boot.startRestAPI`, one `RestAPIVerticle`,
zero event-bus usage. The two worlds touch only at the REST→JADE
boundary (reflection into `ContainerController.myImpl`), and
`DFRequestAgent` bridges them with a `Thread.sleep(50)` busy-poll.

Problems this proposal addresses:

- The agent programming model is thread-and-lock based; behaviours use
  cooperative scheduling but the underlying primitive is a parked thread,
  not an event-driven handler. Future agentic patterns (LLM tool-call
  loops, multi-agent orchestration, streaming) map more naturally to
  message-driven actors than to blocking threads.
- Two disconnected fabrics: JADE's `MessagingService`/`MessageManager`
  for agent-to-agent, and Vert.x's event bus (unused) for REST/WS. No
  unified message surface.
- REST↔agent impedance mismatches (`DFRequestAgent` poll, ~42 handlers
  calling JADE directly on the event loop without `executeBlocking`).

## Decision

### Phase 1 — runtime swap (flag-gated, backward-compatible)

1. **One verticle per agent.** Each agent deploys as an `AgentVerticle`;
   lifecycle maps to deploy (born) / undeploy (`takeDown`). Deployment
   IDs are namespaced per container (`c/<container>/a/<agent>`); scenario
   kill = undeploy by prefix.

2. **One Vert.x per JVM.** The single `Vertx` instance (created in
   `Boot` or `Runtime`) is shared by the REST verticle and all
   containers' agent verticles. The event bus is local-only (no
   clustering dependency).

3. **Per-step dispatch (drain task).** Agents no longer keep a permanent
   thread. Every wake event (`postMessage`/`doWake`,
   `Scheduler.add`/`restart`, timer) calls `ensureScheduled()`: if no
   drain task is running for that agent, one is submitted to a per-agent
   serial executor (virtual threads). The drain task runs
   `{ schedule() → actionWrapper() → blocked-check }` until the
   scheduler is empty, then marks idle and exits. A hot
   `CyclicBehaviour` spins in its drain task the same way it spins on
   its thread today — faithfully.

   The event loop **never runs user code**. It receives bus/timer events,
   performs the `ensureScheduled()` decision (µs), and returns. Dev-team
   file I/O and 600-second `opencode` CLI calls run on the pooled
   virtual thread inside the drain task.

   `blockingReceive` takes the existing foreign-thread branch
   (`waitUntilWake`, `Agent.java:2137`) — JADE already supports this for
   `GatewayAgent`/`DFRequestAgent`.

4. **Old JADE API keeps working.** `addBehaviour`, `blockingReceive`,
   `doWait`, `TickerBehaviour`, `CyclicBehaviour` — all preserved.
   Phase 1 is invisible to agent authors.

5. **Profile flag.** `-agent-runtime thread|verticle` (default `thread`),
   implemented in the `jade` module (needs package-level access to
   `Agent`/`AgentContainerImpl`). CI matrix exercises both runtimes.

6. **Shell model as documented fallback.** If the thread-affinity audit
   (see below) surfaces a blocker for per-step dispatch, retreat to the
   shell model: verticle wraps a persistent per-agent virtual thread;
   `Agent.run()` loop untouched; verticle owns lifecycle/address only.
   Amend this ADR before prototyping.

7. **Phase 1 scope: runtime swap only.** Delivery path
   (`MessageManager`/`OutBox`/deliverers), monitoring
   (`MessageTrafficMonitor`), REST handlers, `TimerDispatcher` — all
   untouched. Event bus carries nothing about agents yet.

### Phase 2 — one fabric + new API (sequential)

8. **Step 1: monitoring bridge.** `MessageTrafficMonitor.notifyMessage`
   also `eventBus.publish`es ACL traffic; `MessagesStreamHandler`
   subscribes from bus. Static fan-out retires. Cheapest proof that one
   fabric works with real traffic.

9. **Step 2: new actor API + port Manager.** `onMessage(ACLMessage)`
   dispatched by pattern matchers (performative, protocol, ontology).
   Handlers run directly on the event loop (enforced non-blocking
   contract; long work via `executeBlocking`/`Future`). FIPA interaction
   protocols are composable handler chains on top, not baked into
   dispatch. Dev-team Manager (deterministic state machine, no LLM) is
   the first port: its `CyclicBehaviour` that switches on `phase` maps
   to `onMessage` + `switch(phase)` + `setState`.

10. **Step 3: delivery migration.** Event bus replaces
    `MessageManager`/`OutBox`/5 deliverer threads for local traffic.
    `agent.send(msg)` → `eventBus.send("c/<container>/a/<receiver>",
    ACLMessage)`. `SYNCH_DELIVERY` → `eventBus.request` (send+reply).
    Remote delivery (cross-JVM via LEAP/MTP) stays on existing I/O path,
    bridged. `MessageTrafficMonitor` fully retires (the bus is the
    monitoring surface).

11. **Step 4: REST handler rework.** Handlers use event-bus
    request/reply instead of direct JADE API calls. `DFRequestAgent`
    poll (`Thread.sleep(50)`) replaced. Remaining `executeBlocking`
    wrappers retire.

### Phase 3 (horizon)

12. **Clustered event bus.** `vertx-cluster` dependency + cluster
    manager for distributed agents. Separate ADR when phase 2 proves
    local fabric.

### Thread-affinity audit (required before prototype)

The per-step dispatch model (decision 3) changes which thread runs agent
code. Four sites in `Agent.java` assume a persistent `myThread`:

| Site | Lines | Migration |
|---|---|---|
| `myThread.interrupt()` | 882, 2409 | Volatile "current step thread" handle; pending-interrupt flag between steps |
| `myThread.join()` in delete/takeDown | 861–894 | Death signal (CountDownLatch/promise) set when drain task observes `lifecycle.alive()==false` after final step |
| `isAlive()` | 797 | Use `myLifeCycle.alive()` |
| `Thread.currentThread().equals(myThread)` | 982, 1186, 2137 | Always "foreign" — verify each branch is correct for the agent's main loop |

Additional scan items:
- `ThreadLocal` usage in `core/`, `devteam/`, `CliBrain` (different
  pooled vthread per step → stale ThreadLocals).
- `ThreadedBehaviourFactory` interaction with the serial gate.

## Testing

### Phase 1 gate (before flipping default)

- All existing test suites green under `-agent-runtime verticle`:
  `DevTeamFlowIntegrationTest` (offline — exercises `blockingReceive`,
  `TickerBehaviour` watchdog, `takeDown`, multi-agent delivery),
  `RestAPIIntegrationTest`, frontend suites unchanged.
- New verticle-specific tests: message ordering per receiver (FIFO),
  drain-task fairness, lifecycle deploy/undeploy/scenario-kill,
  interrupt/abort of a blocked agent.
- Vert.x blocked-thread-checker enabled and clean.
- CI matrix (`-agent-runtime=thread|verticle`) in both `ci.yml` and
  nightly `real-it.yml`. Flip default to `verticle` only after N
  consecutive green nightlies on both axes.

### Phase 2

- Monitoring bridge: live-messages UI functional via event bus; ordering
  matches current static fan-out.
- New API: Manager ported, all dev-team phases pass.
- Delivery migration: ordering tests, `SYNCH_DELIVERY` semantics.

## Consequences

- Phase 1 is invisible to agent authors — old API, same semantics, flag
  defaults off. Risk contained by the CI matrix and kill-switch.
- Parked agent threads disappear: idle agents consume only the verticle
  handle (no stack, no monitor wait). Thousands of agents become cheaper
  (already cheap with virtual threads, but structurally cleaner).
- The per-step dispatch model is the foundation for the phase 2 actor
  API: drain task → inline handler on event loop is just "remove the
  executor, enforce non-blocking."
- `TimerDispatcher` and `MessageManager`/`OutBox`/5 deliverers survive
  phase 1 untouched; they retire in phase 2 steps 1 and 3.
- Phase 2 TBD items: failure semantics in new API (catch-and-continue vs
  supervisor), FIPA enforcement level (strict vs optional protocol
  typing), ACL serialization format for bus (local bus passes objects;
  codec needed for clustering in phase 3).

## Spike plan

1. **ADR-0004 draft** (this document) — captures decisions while fresh.
2. **Thread-affinity audit** (parallel with draft) — deep scan of
   `myThread`/`currentThread`/`ThreadLocal` across `core/` + dev-team.
   If blocker found: amend decision 3 to shell model (decision 6).
3. **Prototype** (after audit) — drain task + `ensureScheduled()` seam +
   flag + `DevTeamFlowIntegrationTest` green under
   `-agent-runtime verticle`.
