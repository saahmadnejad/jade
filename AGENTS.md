# AGENTS.md — Jade Project Guidelines

## Project Overview

Jade is a fork of JADE (Java Agent DEvelopment Framework) — a multi-agent framework under the
`io.donbee.jade` package, targeting Java 21 with virtual threads.

- **Backend** (`backend/`) — Java/Maven, builds to `backend/target/backend.jar` (shade uber jar)
- **Frontend** (`frontend/`) — pnpm monorepo: React app at **`frontend/apps/webapp/`**
  (package name: `webapp`) + shared TS lib at `frontend/packages/shared/` (package name: `shared`)
- **Migration state**: old Swing GUI tools are being replaced by a Vert.x REST API + React UI.
  Track progress in `docs/old-gui-functionalities.md`; API specs live in `docs/api/*.md`.

## Build & Test Commands

```bash
# Backend
cd backend && mvn package -DskipTests     # Build uber jar
cd backend && mvn test                    # 38 test files exist under src/test/java
# If mvn picks the wrong JDK:
cd backend && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test

# Frontend (from frontend/)
pnpm --filter webapp dev          # Vite dev server, port 3000
pnpm --filter webapp test:run     # webapp tests (non-watch)
pnpm --filter shared test:run     # shared package tests
pnpm --filter webapp exec eslint src --ext .ts,.tsx   # lint webapp

# Run a single Vitest file:
cd frontend/apps/webapp && npx vitest run src/pages/AgentsPage.test.tsx

# Docker (podman, compose file at repo root)
podman compose up --build -d
podman compose down
```

### Command gotchas

- The root `frontend/package.json` scripts (`pnpm build`, `pnpm dev`) use
  `pnpm --filter frontend`, which matches **nothing** — no package is named `frontend`.
  Always filter by `webapp` or `shared`.
- `pnpm --filter webapp test` runs Vitest in **watch mode** and will hang a non-interactive
  session. Use `test:run` or `npx vitest run`.
- Frontend tests mock the API layer with `vi.mock('shared/api/factory', ...)`. New API methods
  must be added to that mock object in every page test or tests crash on undefined calls.

## Architecture Notes

### Backend (`io.donbee.jade.*`)

- Main class: `io.donbee.jade.Boot` (CLI args + REST startup). Local run:
  `cd backend && mvn compile exec:java -Dexec.mainClass="io.donbee.jade.Boot"`
- REST API: Vert.x server on port 8080 (`Profile.REST_PORT`, CLI `-rest-port <n>`), started only
  on the Main Container. ~38 endpoints under `/api/*` covering platform, containers, agents,
  tools, remote platforms, and DF.
- Wiring: `rest/RestAPIVerticle.java` only wires routes; route paths are constants in
  `rest/ApiRoutes.java`; logic lives in one class per endpoint in `rest/handler/`; delegation to
  service layer (`PlatformService`, DF services).
- Dependencies: JacORB 3.9, commons-codec 1.18.0, Vert.x 4.5.10.
- Threads use `Thread.ofVirtual()` (Java 21 virtual threads).

### Frontend

- React 18 + TypeScript + Vite 5 + MUI (v9.x, dark theme), ESLint strict.
  Note: MUI deps are declared at the `frontend/` workspace root, not in `apps/webapp/package.json`.
- Pages live in `frontend/apps/webapp/src/pages/` (AgentsPage, ContainersPage, PlatformsPage,
  DFPage, DashboardPage, ToolsPage); each page has a co-located `.test.tsx`.
- API access is via the `shared` package: `import { api } from 'shared'`. Domain clients
  (`api.agents`, `api.containers`, `api.platforms`, `api.df`, `api.tools`, `api.platform`) are
  built by `packages/shared/src/api/factory.ts` over an injectable `HttpClient`
  (`http-client.ts`, axios). Types live in `packages/shared/src/api/types.ts`.
- SPA only — no server-side routing; nginx serves index.html fallback and proxies `/api`.

### Docker

- Dockerfiles in `docker/`, compose file at repo root.
- Backend RMI 1099 → host 10990; frontend nginx 80 → host 3000; backend REST reachable at
  `http://localhost:8080/api`.

### Key Files

- `backend/src/main/java/io/donbee/jade/rest/RestAPIVerticle.java` — REST route wiring
- `backend/src/main/java/io/donbee/jade/rest/ApiRoutes.java` — all route path constants
- `backend/src/main/java/io/donbee/jade/rest/handler/` — one handler class per endpoint
- `backend/src/main/java/io/donbee/jade/core/Profile(Impl).java` — config constants/impl
- `frontend/packages/shared/src/api/factory.ts` — builds the `api` object
- `docs/old-gui-functionalities.md` — migration tracker (update checkboxes when features ship)
- `docs/api/*.md` — REST endpoint specs; update before/with code changes

## Coding Standards

### Architecture (SOLID)

- **SRP**: `RestAPIVerticle` wires routes only; handler logic goes in dedicated handler classes;
  handlers delegate to service-layer interfaces.
- **OCP/DIP**: new endpoints = new handler classes; high-level code depends on abstractions
  (e.g., shared package's `HttpClient` interface), injected via constructors.
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

- Backend layout: mirror packages under `src/test/java`; unit-test handlers with mocked
  services; integration-test endpoints against an in-memory/mock runtime.
- Frontend layout: `apps/webapp/src/**/*.test.tsx`, `packages/shared/src/**/*.test.ts`;
  mock axios/API factory, cover success, error, and disabled-button/validation states.

### REST API Design Rules

1. Resource naming (`/api/agents`, `/api/containers`); HTTP verbs map to CRUD/PATCH semantics.
2. Error responses always: `{"error": "message", "code": <status>}` via the global failure
   handler; all responses JSON.
3. Every endpoint documented with input/output JSON schema in `docs/api/`.
4. **Docs consistency rule**: update the doc spec first, then verify docs match code before any
   commit touching the REST API. No commit is complete without this check.

### Old GUI JavaDoc Requirement (REST handlers)

Every REST handler class in `io.donbee.jade.rest.handler` **must** include a JavaDoc block with
a `<b>Old GUI implementation</b>` section that names:

- the old Swing class performing the same function,
- the old method/callback (with file:line where known),
- the FIPA protocol / ontology used,
- the service-layer method this handler delegates to.

For genuinely new endpoints (no Swing equivalent), note "No direct Swing GUI equivalent" and
name the closest old code as context. Example:

```java
/**
 * Handler for DELETE /api/agents/:name — kill an agent.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.KillAction} (doAction) called
 * {@code rma.killAgent(id)} ({@code rma.java:644}), which sent a
 * {@code KillAgent} action via {@code JADEManagementOntology} to the
 * AMS through an {@code AMSClientBehaviour}. This handler delegates to
 * {@code PlatformService#killAgent()} which uses
 * {@code AgentManager#kill()}.</p>
 */
```

Old Swing sources for reference: `backend/src/main/java/io/donbee/jade/tools/` (rma, dfgui,
sniffer, introspector, logging, DummyAgent) and `backend/src/main/java/io/donbee/jade/gui/`.
