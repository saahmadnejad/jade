# AGENTS.md — Jade Project Guidelines

## Project Structure

**Backend** (`backend/`) — Maven multi-module: `fipa` (CORBA FIPA lib), `llm` (framework-agnostic LLM), `jade` (platform), `examples` (scenarios).  
**Frontend** (`frontend/`) — pnpm monorepo: `apps/webapp` (React + Vite) + `packages/shared` (TS library).  
**Docker** — Backend runs JADE platform with REST API; frontend serves React SPA via nginx; `9router` service proxies LLM providers.

## Build & Run

```bash
# Backend: build (Java 21 REQUIRED — see JDK gotcha below)
cd backend && mvn package -DskipTests
# Full test suite
cd backend && mvn test
# Single module
cd backend && mvn test -pl llm   # or -pl examples, -pl jade

# Run locally (main container + REST API on :8080)
cd backend && mvn compile exec:java -Dexec.mainClass="io.donbee.jade.Boot"

# Frontend: dev server on :3000
cd frontend && pnpm --filter webapp dev
# Frontend tests (non-watch)
pnpm --filter shared test:run
pnpm --filter webapp test:run
# Lint (webapp only)
pnpm --filter webapp lint

# Docker (compose at repo root; NINEROUTER_API_KEY is REQUIRED or the brain probe 401s)
export NINEROUTER_API_KEY=$(grep 'llm.api.key' backend/examples/conf/secrets-local.properties | cut -d= -f2)
docker compose up -d --build    # README says `podman compose`; docker works identically
```

**JDK gotcha**: a JRE-only Java 21 on `PATH`/`JAVA_HOME` (e.g. distro `java-21-openjdk` without `javac`) breaks `mvn` with cryptic compile errors. Ensure `JAVA_HOME` points at a full JDK 21 that has `bin/javac` (sdkman JDKs do).

**Gotchas**

- Root `pnpm build`/`pnpm dev` scripts use `--filter frontend` — matches nothing. Always `--filter webapp` or `--filter shared`.
- `pnpm --filter webapp test` runs in **watch mode** (hangs an agent). Use `test:run`.
- Frontend tests mock `shared/api/factory`; add new API methods to the mock in every test file or tests crash.
- Docker Hub pulls occasionally fail (TLS timeout to auth.docker.io) — retry; base images are cached after first pull.
- `docker compose up` without `--build` reuses the stale `jade-backend:latest` image even after `mvn package` — source changes need `up -d --build` (or `docker compose build backend` first).
- Frontend `useFeedback()` in `NotificationSnackbar.tsx` must keep `notify`/`close` as stable `useCallback`s — unstable identities caused fetch loops that spammed the scenarios/instances API.

## Architecture

### Backend (`io.donbee.jade.*`)

- **Entry**: `io.donbee.jade.Boot` (CLI args → `ProfileImpl` → `Runtime`). `-conf` loads properties file; `-name`, `-container`, `-host`, `-port`, `-rest-port` override defaults.
- **REST API**: Vert.x 5.1.6 on :8080 (configurable `-rest-port`). ~44 endpoints under `/api/*`. Main Container only. Route wiring: `RestAPIVerticle`; paths: `ApiRoutes`; one handler class per endpoint in `rest/handler/`.
- **Blocking rule**: Vert.x handlers that do blocking work (LLM probes, container create/kill, synchronized service calls) MUST use `ctx.vertx().executeBlocking(...)` — the event loop froze for 130s+ before this rule existed.
- **Main Container**: AMS, DF, REST API, all UI/REST-deployed agents. Each scenario instance gets its own container (`scenario-<name>`).
- **Message capture**: `MessageTrafficMonitor` (static JVM-wide listeners) fires in `MessagingService.CommandSourceSink` — captures messages from ALL in-process containers, including scenario containers. Streams via `WS /api/messages/stream`.
- **Scenario SPI**: any jar with `META-INF/services/io.donbee.jade.rest.scenario.Scenario` appears in `/api/scenarios`.
- **LLM brains** (`backend/llm`): `Brain` interface, `LangChain4jBrain` (OpenAI-compatible, tool-calling agent loop, 429 retry waiting provider's "reset after Ns" window), `BashTool` (escape-aware JSON scanner — heredoc/`\n`/`\"` handling has regression tests; touch it and run `LangChain4jBrainTest`), `FallbackBrain` (chains brains on `BrainException`).
- **Threads**: all agent/platform threads are virtual (`Thread.ofVirtual()`) — `kill -3` thread dumps show carrier frames only, not virtual-thread stacks.
- **Deps**: JacORB 3.9, commons-codec 1.18.0, Vert.x 5.1.6, langchain4j 0.35.0. langchain4j 0.35 API: `generate(List<ChatMessage>, List<ToolSpecification>)`, `AiMessage.toolExecutionRequests()`, `ToolExecutionResultMessage.from(req, result)`.

### Dev-team scenario (`backend/examples/.../devteam/`)

- 5 agents: Manager (deterministic state machine, no LLM) → Architect → Implementer → Tester → Reviewer. Manager phases: CLARIFY→DESIGN→IMPLEMENT→TEST→REVIEW→PUBLISH with per-phase deadlines (`enterPhase()`) + TickerBehaviour watchdog.
- Scenario start body: `{"instanceName": "...", "config": {...}}` — config MUST be nested under `"config"`; a flat body is silently ignored → empty brief → instance FAILED.
- Agent workspace: `/tmp/jade-devteam-<instance>/` inside the backend container.
- Model params (`*Model`) must name a model that actually responds on the configured 9router — verify with `curl http://localhost:20129/v1/models` or the dashboard before starting a scenario. Free-pool models come and go; dead defaults surface as 401/429 at the first role-agent call.
- Peer messages: `PEER_PROTOCOL` = `FIPANames.InteractionProtocol.FIPA_REQUEST` (exact `fipa-request` token — case-sensitive, never hand-write it). Manager filters its own `dt-*` conversation IDs and ignores `dt-peer-*`.
- `githubOrg` defaults empty (skips publish). Publishing requires `GH_TOKEN` env in the backend container + non-empty `githubOrg`.

### Frontend

- **Stack**: React 18 + TypeScript + Vite 5 + MUI 9.x (dark theme). ESLint strict.
- **Monorepo**: `webapp` consumes `shared` (workspace:*); MUI deps declared at `frontend/` root, not in `apps/webapp/package.json`.
- **API**: `shared` bundles domain clients (`api.agents`, `api.platforms`, `api.df`, `api.messages`, `api.scenarios`) over `HttpClient` (axios) with injectable interface.
- **Live messages**: `subscribeMessagesStream()` (WebSocket, auto-reconnect).
- **Routing**: SPA only; nginx serves `index.html` fallback.

### Docker

- **Backend image**: multi-stage (Maven build → JRE 21 Alpine), runs `java -cp app.jar:examples.jar:llm.jar io.donbee.jade.Boot`. Includes `gh` CLI + opencode + python3/node for agent tooling.
- **Ports**: 9router 20128→host:20129 (dashboard at http://localhost:20129/dashboard); backend RMI 1099→10990, REST 8080→8080; frontend nginx 80→3000.
- **9router data**: compose mounts `${NINEROUTER_DB_DIR:-~/.9router/db}` so the container reuses host 9router provider keys. DB is sqlite (`data.sqlite` in that dir; if no sqlite3 CLI, use python3's `sqlite3` module).

## Secrets (ADR-0002 — never commit)

- Real 9router key lives ONLY in gitignored `backend/examples/conf/secrets-local.properties`. Template: `secrets-local.properties.example` (placeholder values only).
- `.gitignore` covers `secrets*.properties`, `secrets-*.properties`, `.project`, `.classpath`, `.settings/`.
- LLM key resolution in agents: `NINEROUTER_API_KEY` env first, then the properties file (`SecretsResolver`).

## Key Files

- `backend/jade/src/main/java/io/donbee/jade/Boot.java` — CLI entry
- `backend/jade/src/main/java/io/donbee/jade/core/Profile*.java` — config constants
- `backend/jade/src/main/java/io/donbee/jade/rest/RestAPIVerticle.java` — route wiring
- `backend/jade/src/main/java/io/donbee/jade/rest/ApiRoutes.java` — route path constants
- `backend/jade/src/main/java/io/donbee/jade/rest/handler/` — one class per endpoint
- `backend/llm/src/main/java/io/donbee/llm/{Brain,LangChain4jBrain,BashTool,FallbackBrain}.java` — LLM stack
- `backend/examples/.../devteam/` — dev-team scenario (Manager/Role agents, scaffolding, secrets)
- `frontend/packages/shared/src/api/factory.ts` — builds `api` clients
- `backend/examples/README.md` — scenario docs (online-shop, dev-team)

## Coding Standards

### Architecture (SOLID)

- **SRP**: `RestAPIVerticle` wires routes only; handler logic in dedicated handler classes; handlers delegate to service-layer interfaces.
- **OCP/DIP**: new endpoints = new handler classes; high-level code depends on abstractions (e.g. `shared`'s `HttpClient` interface), injected via constructors.

### Tests (mandatory for new code)

Arrange-Act-Assert with Given-When-Then naming:

```java
@Test
void Given_UserIsAuthenticated_When_AgentListRequestedWithDetail_Then_ResponseIncludesStateAndOwnership() {
    // --- Arrange --- / --- Act --- / --- Assert ---
}
```

```typescript
it('Given an authenticated user, When the agent list endpoint is called with detail=true, Then the response includes agent state and ownership', () => {
  // Arrange / Act / Assert
});
```

- Backend: mirror packages under `src/test/java`; unit-test handlers with mocked services (Mockito); integration-test endpoints in `RestAPIIntegrationTest`.
- Frontend: `apps/webapp/src/**/*.test.tsx`, `packages/shared/src/**/*.test.ts`; mock axios/API factory; cover success, error, and disabled-button/validation states.

### REST API Design Rules

1. Resource naming (`/api/agents`, `/api/containers`); HTTP verbs map to CRUD semantics.
2. Error responses always `{"error": "message", "code": <status>}` via the global failure handler.
3. Every endpoint documented with input/output JSON schema in `docs/api/`.
4. **Docs consistency rule**: update the doc spec first, then verify docs match code before any commit touching the REST API.

### No machine-specific values in committed docs

Never write host-specific values into README/AGENTS/docs/ADR files: absolute home paths (`/home/<user>/...`, `~/.sdkman/...`), local JDK paths, hostnames, usernames, real API keys/tokens, or ephemeral model names that only work on one machine's provider pool. Use repo-relative paths, env var names, and "verify via `/v1/models`" instead. Tracked docs must let any internet reader run the project.

### Old GUI JavaDoc Requirement (REST handlers)

Every REST handler class in `io.donbee.jade.rest.handler` must include JavaDoc with a `<b>Old GUI implementation</b>` section naming:
- the old Swing class + method (with file:line where known),
- the FIPA protocol / ontology used,
- the service-layer method this handler delegates to.

New endpoints without a Swing equivalent: note "No direct Swing GUI equivalent" + closest old code as context.

Old Swing sources: `backend/jade/src/main/java/io/donbee/jade/tools/` (rma, dfgui, sniffer, introspector, logging, DummyAgent) and `backend/jade/src/main/java/io/donbee/jade/gui/`.

## Agent skills

- **Issue tracker**: GitHub Issues (saahmadnejad/jade) via `gh` CLI — `docs/agents/issue-tracker.md`.
- **Triage labels**: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` — `docs/agents/triage-labels.md`.
- **Domain docs**: single-context `CONTEXT.md` + `docs/adr/` at repo root — `docs/agents/domain.md`.
