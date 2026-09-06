# AGENTS.md — Jade Project Guidelines

## Project Structure

**Backend** (`backend/`) — Maven multi-module: `fipa` (CORBA FIPA lib), `llm` (framework-agnostic LLM), `jade` (platform), `examples` (scenarios).  
**Frontend** (`frontend/`) — pnpm monorepo: `webapp` (React + Vite) + `shared` (TS library).  
**Docker** — Backend runs JADE platform with REST API; frontend serves React SPA via nginx.

## Build & Run

```bash
# Backend: build artifacts (uber jar + examples + llm)
cd backend && mvn package -DskipTests
# Run locally (main container + REST API on :8080)
cd backend && mvn compile exec:java -Dexec.mainClass="io.donbee.jade.Boot"

# Frontend: dev server on :3000
cd frontend && pnpm --filter webapp dev

# Test: backend (Java 21 required)
cd backend && mvn test
# Frontend: unit tests non-watch
pnpm --filter shared test:run
pnpm --filter webapp test:run

# Docker (compose at repo root)
podman compose up --build -d
```

**Gotchas**

- Root `pnpm build`/`pnpm dev` scripts use `--filter frontend` — matches nothing. Always `--filter webapp` or `--filter shared`.
- `pnpm --filter webapp test` runs in **watch mode** (hangs). Use `test:run`.
- Frontend tests mock `shared/api/factory`; add new API methods to mock in every test or crash.

## Architecture

### Backend (`io.donbee.jade.*`)

- **Entry**: `io.donbee.jade.Boot` (CLI args → `ProfileImpl` → `Runtime`). `-conf` loads properties file; `-name`, `-container`, `-host`, `-port`, `-rest-port` override defaults.
- **REST API**: Vert.x 5.1.6 server on port 8080 (configurable via `-rest-port`). ~44 endpoints under `/api/*`. Started only on Main Container.
- **Main Container**: Hosts AMS, DF, all agents deployed via `/api/agents`, and REST API. All bundled examples run here.
- **Container isolation**: Each scenario instance runs in its own container (`scenario-<name>`); agents started via UI/REST go to Main Container.
- **Message capture**: `MessageTrafficMonitor` captures FIPA ACL messages dispatched by Main Container agents only. Streams via `WS /api/messages/stream`.
- **Scenario SPI**: Any jar with `META-INF/services/io.donbee.jade.rest.scenario.Scenario` implementation appears in `/api/scenarios`.
- **Threads**: All use `Thread.ofVirtual()` (Java 21).
- **Deps**: JacORB 3.9, commons-codec 1.18.0, Vert.x 5.1.6, langchain4j 0.35.0 (in `llm`).

### Frontend

- **Stack**: React 18 + TypeScript + Vite 5 + MUI 9.x (dark theme). ESLint strict.
- **Monorepo**: `webapp` consumes `shared` (workspace:*); MUI deps declared at `frontend/` root, not in `apps/webapp/package.json`.
- **API**: `shared` bundles domain clients (`api.agents`, `api.platforms`, `api.df`, `api.messages`, `api.scenarios`) over `HttpClient` (axios) with injectable interface.
- **Live messages**: `subscribeMessagesStream()` (WebSocket, auto-reconnect).
- **Routing**: SPA only; nginx serves `index.html` fallback.

### Docker

- **Backend image**: Multi-stage (Maven → JRE 21 Alpine); entries: `java -cp app.jar:examples.jar:llm.jar io.donbee.jade.Boot -host localhost -local-host localhost`.
- **Frontend image**: Multi-stage (Node → pnpm → Vite → nginx); serves from `/usr/share/nginx/html`.
- **Ports**: Backend RMI 1099→host:10990, REST 8080→host:8080; frontend nginx 80→host:3000.
- **Service**: `9router` (dev tool on :20129) hosts free LLMs (combo-coding, poolside models).

## Key Files

- `backend/jade/src/main/java/io/donbee/jade/Boot.java` — CLI entry
- `backend/jade/src/main/java/io/donbee/jade/core/Profile*.java` — config constants
- `backend/jade/src/main/java/io/donbee/jade/rest/RestAPIVerticle.java` — route wiring
- `backend/jade/src/main/java/io/donbee/jade/rest/ApiRoutes.java` — all route path constants
- `backend/jade/src/main/java/io/donbee/jade/rest/handler/` — one class per REST endpoint
- `frontend/packages/shared/src/api/factory.ts` — builds `api` clients
- `backend/examples/README.md` — scenario docs (online-shop, dev-team)

## Coding Standards

### Architecture (SOLID)

- **SRP**: `RestAPIVerticle` wires routes only; handler logic goes in dedicated handler classes; handlers delegate to service-layer interfaces.
- **OCP/DIP**: new endpoints = new handler classes; high-level code depends on abstractions (e.g., shared package's `HttpClient` interface), injected via constructors.
- **ISP/LSP**: split fat manager interfaces; mocks must be behaviorally substitutable.

### Tests (mandatory for new code)

Follow Arrange-Act-Assert with Given-When-Then naming:

```java
// Java / JUnit 5 + Mockito
@Test
void Given_UserIsAuthenticated_When_AgentListRequestedWithDetail_Then_ResponseIncludesStateAndOwnership() {
    // --- Arrange --- / --- Act --- / --- Assert ---
}
```

```typescript
// Vitest + React Testing Library
it('Given an authenticated user, When the agent list endpoint is called with detail=true, Then the response includes agent state and ownership', () => {
  // Arrange / Act / Assert
});
```

- Backend layout: mirror packages under `src/test/java`; unit-test handlers with mocked services; integration-test endpoints against an in-memory/mock runtime.
- Frontend layout: `apps/webapp/src/**/*.test.tsx`, `packages/shared/src/**/*.test.ts`; mock axios/API factory, cover success, error, and disabled-button/validation states.

### REST API Design Rules

1. Resource naming (`/api/agents`, `/api/containers`); HTTP verbs map to CRUD/PATCH semantics.
2. Error responses always: `{"error": "message", "code": <status>}` via the global failure handler; all responses JSON.
3. Every endpoint documented with input/output JSON schema in `docs/api/`.
4. **Docs consistency rule**: update the doc spec first, then verify docs match code before any commit touching the REST API. No commit is complete without this check.

### Old GUI JavaDoc Requirement (REST handlers)

Every REST handler class in `io.donbee.jade.rest.handler` **must** include a JavaDoc block with a `<b>Old GUI implementation</b>` section that names:
- the old Swing class performing the same function,
- the old method/callback (with file:line where known),
- the FIPA protocol / ontology used,
- the service-layer method this handler delegates to.

For genuinely new endpoints (no Swing equivalent), note "No direct Swing GUI equivalent" and name the closest old code as context. Example:

```java
/**
 * Handler for DELETE /api/agents/:name — kill an agent.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.KillAction} (doAction) called
 * {@code rma.killAgent(id)} ({@code rma.java:644}), which sent a
 * {@code KillAgent} action via {@code JADEManagementOntology} to the
 * AMS through an {@code AMSClientBehaviour}. This handler delegates to
 * {@code PlatformService#killAgent()} which uses {@code AgentManager#kill()}.</p>
 */
```

Old Swing sources for reference: `backend/src/main/java/io/donbee/jade/tools/` (rma, dfgui, sniffer, introspector, logging, DummyAgent) and `backend/src/main/java/io/donbee/jade/gui/`.

## Agent skills

### Issue tracker
Issues live in this repo's GitHub Issues (saahmadnejad/jade), managed via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels
Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs
Single-context: one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
