# Agent as Vert.x verticle — event-driven agent runtime

## Status

Accepted (spike complete: thread-affinity audit and drain-task prototype verified green).

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

### Thread-affinity audit (completed — no blockers)

The per-step dispatch model (decision 3) changes which thread runs agent
code. Deep scan of `Agent.java`, `core/`, `devteam/`, `llm/`, and `ThreadedBehaviourFactory`
confirmed **no blockers**. Decision 3 is viable; retreat to shell model (decision 6) is not required.

#### 1. `Agent.java` persistent-thread sites

| Site | Lines | Finding & Migration |
|---|---|---|
| `isAlive()` | 797 | Currently checks `myThread != null && myThread.isAlive()`. In per-step dispatch, an idle agent has no active thread, causing `isAlive()` to return `false`. **Critical impact**: `ThreadedBehaviourFactory:428` calls `checkAgentTerminated()` which checks `!myAgent.isAlive()`, prematurely killing threaded behaviours if `isAlive()` is false between steps. **Migration**: Decouple from thread; return `myLifeCycle != null && myLifeCycle.alive()`. |
| `join()` in delete/takeDown | 861–894 | Called by `AgentContainerImpl.shutDown()` (line 657) and `MainContainerImpl.removeLocalContainer()` (lines 163, 169) to wait for agent termination. If `myThread` is transient, joining a single step thread does not wait for full teardown, and if `myThread` is null it returns `true` immediately before `clean()` finishes. **Migration**: Use a termination latch/future (`CompletableFuture<Void>`). When the agent transitions to deleted and finishes `myLifeCycle.end()` (`clean()`), complete the future. `join()` waits on this future with timeout. |
| `interruptThread()` / `myThread.interrupt()` | 882, 2409 | Line 2409 directly calls `myThread.interrupt()`. If the agent is idle between steps, `myThread` is null and throws NPE. **Migration**: Guard against null: `Thread t = myThread; if (t != null) t.interrupt();`. Maintain a pending-interrupt flag between steps. While a drain step runs, bind volatile `myThread = Thread.currentThread()` so in-flight blocking calls (e.g. LLM I/O, `blockingReceive`) receive interrupts promptly, clearing `myThread = null` in a finally block upon step completion. |
| `Thread.currentThread().equals(myThread)` | 982, 1186, 2137 | **Line 982** (`changeStateTo`): only interrupts thread if called from an external thread. Binding `myThread = Thread.currentThread()` during step execution preserves this.<br>**Line 1186** (`doWait`): no-op if foreign. Binding `myThread` during step execution preserves `doWait` for in-step callers.<br>**Line 2137** (`blockingReceive`): if foreign, calls `waitUntilWake` which parks on `msgQueue` monitor without updating lifecycle state; if current, calls `doWait` which marks `AP_WAITING`. Both park on `msgQueue` and wake on `doWake()`. On virtual threads inside drain tasks, both branches work safely. |
| Idle `doDelete()` / `takeDown()` trigger | 980–1000, 1618, 1776 | **Critical architectural finding**: In standard JADE, an idle agent is parked inside `schedule()` -> `idle()`. Calling `doDelete()` interrupts that parked thread, the `run()` loop catches `InterruptedException`, evaluates `!alive()`, and calls `myLifeCycle.end()` -> `takeDown()`. In per-step dispatch, an idle agent has no thread running. If `doDelete()` is called while idle, `takeDown()` will **never execute** unless `doDelete()` / state transition explicitly calls `ensureScheduled()` to dispatch a final termination task that executes `clean(true)` and signals the termination latch. |

#### 2. `ThreadLocal` audit

- `core/`:
  - `AgentReplicationHandle.java:37`: `static ThreadLocal<Boolean> replicatedCalls`. Bound strictly around synchronous replicated method invocation (`enterReplicatedCall` ... `exitReplicatedCall` in `finally`). Never leaks across steps.
  - `DBKB.java:63`: `ThreadLocal connections`. Used only in optional JDBC-backed DF knowledge base (`DBKB`), not in default in-memory DF (`MemoryKB`). Re-authenticates lazily per thread.
  - `DeliveryTracing.java:9`: `static final ThreadLocal<DeliveryTracing> tracing`. Used strictly inside `MessageManager` deliverer threads, not on agent threads.
- `devteam/`: Zero `ThreadLocal` usage.
- `llm/` (`CliBrain`, `RetryingBrain`, `LangChain4jBrain`, `BashTool`): Zero `ThreadLocal` usage.
- `fipa/`: Zero `ThreadLocal` usage.
- **Verdict**: No `ThreadLocal` leaks or staleness across pooled virtual threads.

#### 3. `ThreadedBehaviourFactory` audit

- `ThreadedBehaviourWrapper` executes `b.actionWrapper()` in its own dedicated virtual thread (`Thread.ofVirtual().start(this)`), while its agent scheduler step is a non-busy `block()`.
- It interacts with `myAgent` exclusively via `checkAgentTerminated()` calling `myAgent.isAlive()`.
- Migrating `isAlive()` to check `myLifeCycle.alive()` guarantees `ThreadedBehaviourFactory` functions identically without premature aborts.
- **Verdict**: Fully compatible with per-step serial dispatch.

#### 4. Dev-team & LLM audit

- `ManagerAgent` is a pure deterministic state machine (`CyclicBehaviour` + `TickerBehaviour`).
- `RoleAgent` delegates to `Brain.think()`. `CliBrain` spawns and joins short-lived virtual threads for stdout/stderr stream consumption; `RetryingBrain` uses `Thread.sleep` for exponential backoff on virtual threads.
- All operations are completely non-reliant on thread identity or thread naming.
- **Verdict**: Fully compatible.

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

1. **ADR-0004 draft** (this document) — Done (captured decisions).
2. **Thread-affinity audit** — Done (audit complete; no blockers; per-step dispatch confirmed viable; see findings above).
3. **Prototype** — Done (drain task + `ensureScheduled()` seam + `-agent-runtime verticle` flag; `AgentVerticleTest` (6 tests) and `DevTeamFlowIntegrationTest` green under both `thread` and `verticle` runtimes).
