# AGENTS.md — Jade Project Guidelines

This document provides guidance for AI agents (like Claude/opencode) working on the Jade project.

## Project Overview

Jade is a fork of JADE (Java Agent DEvelopment Framework) — a multi-agent system framework
under the `ir.donbee.jade` package. It targets Java 21+ with virtual threads.

The project has two main parts:
1. **Backend** (`backend/`) — Java/Maven, 1015 source files, builds to `backend/target/backend.jar`
2. **Frontend** (`frontend/`) — pnpm monorepo: React app (`apps/frontend/`) + shared TS lib (`packages/shared/`)

## Build & Test Commands

```bash
# Backend
cd backend && mvn package -DskipTests        # Build uber jar
cd backend && mvn test                          # Run tests (if any exist)

# Frontend
cd frontend && pnpm install && pnpm build      # Build for production
cd frontend && pnpm --filter frontend dev       # Dev server (port 3000)

# Docker
podman compose up --build -d                  # Start both containers
podman compose down                           # Stop containers
```

## Key Conventions

### Backend (Java)

- **Source root**: `backend/src/main/java/io/donbee/jade/`
- **Java version**: 21 (virtual threads via `Thread.ofVirtual()`)
- **Build tool**: Maven (`pom.xml` at `backend/pom.xml`)
- **Main class**: `io.donbee.jade.Boot` (declared in pom.xml shade plugin)
- **Dependencies**: JacORB 3.9, commons-codec 1.18.0, Vert.x 4.5.10 (core, web, web-client)
- **Package**: All code under `io.donbee.jade.*`
- **REST API**: Built-in Vert.x REST server, started on Main Container. Endpoints:
  - `GET /api/health` — health check
  - `GET /api/version` — JADE version info
  - `GET /api/platform` — platform metadata (ID, container name, AMS, DF)
  - `GET /api/agents` — list of agents in the main container
  - Configurable via `Profile.REST_PORT` (default 8080, pass as `-rest-port <n>` on CLI)
- **REST source**: `backend/src/main/java/io/donbee/jade/rest/`

### Frontend (TypeScript)

- **Package manager**: pnpm (workspaces at `frontend/` root)
- **Build tool**: Vite 5
- **Framework**: React 18 with TypeScript
- **Linting**: ESLint + TypeScript, strict mode
- **File naming**: `.ts` for modules, `.tsx` for React components

### Docker

- Dockerfiles live in `docker/`
- `docker-compose.yml` lives at the **repo root**
- Backend: multi-stage (Maven → JRE 21 Alpine)
- Frontend: multi-stage (Node → pnpm → Vite build → nginx Alpine)
- Backend exposes RMI port 1099 (mapped to host 10990)
- Frontend exposed on port 80 (mapped to host 3000)

## Common Tasks

### Adding a new Docker change
1. Edit `docker/Dockerfile.{backend|frontend}` or `docker-compose.yml`
2. Run `podman compose up --build -d` to rebuild and restart
3. Verify with `podman compose ps` and `curl`

### Adding a frontend page
1. Create a component in `frontend/apps/frontend/src/`
2. Use the `shared` package for API calls: `import { fetchData } from 'shared'`
3. Route via Vite's `index.html` entry point (SPA — no server-side routing)

### Modifying the backend
1. Edit Java files in `backend/src/main/java/io/donbee/jade/`
2. Test locally: `cd backend && mvn compile exec:java -Dexec.mainClass="io.donbee.jade.Boot"`
3. Rebuild Docker with `podman compose up --build -d`

## Important Files

- `backend/src/main/java/io/donbee/jade/Boot.java` — Entry point, CLI arg parsing, REST API startup
- `backend/src/main/java/io/donbee/jade/rest/RestAPIVerticle.java` — Vert.x REST verticle
- `backend/src/main/java/io/donbee/jade/core/Profile.java` — Profile constants (incl. `REST_PORT`)
- `backend/src/main/java/io/donbee/jade/core/ProfileImpl.java` — Profile implementation
- `backend/src/main/java/ir/donbee/jade/core/FullResourceManager.java` — Virtual thread management
- `frontend/apps/frontend/src/App.tsx` — Root React component
- `frontend/packages/shared/src/api/client.ts` — Axios API client
- `docker/Dockerfile.backend` — Backend Docker build
- `docker/Dockerfile.frontend` — Frontend Docker build

## Coding Standards (Software Architect Role)

When working on this project, act as a **software architect** who values robustness, reusability, and adherence to SOLID principles:

### SOLID Principles
- **Single Responsibility (SRP)**: Each class should have exactly one reason to change. Extract route handlers, data services, and model builders into separate classes. E.g., `RestAPIVerticle` should only wire routes; delegate handler logic to dedicated handler/service classes.
- **Open/Closed (OCP)**: Use interfaces and abstractions so new endpoints can be added without modifying existing handler classes.
- **Liskov Substitution (LSP)**: When implementing service interfaces, ensure substitutability (e.g., mock services for testing should behave identically).
- **Interface Segregation (ISP)**: Avoid fat interfaces. Split `AgentManager` into focused interfaces if possible.
- **Dependency Inversion (DIP)**: High-level handlers should depend on abstractions (interfaces), not concrete JADE backend classes. Inject dependencies via constructors.

### Test-First Approach with AAA Pattern
All new code must include tests following the **Arrange-Act-Assert** pattern:

#### Naming Convention (TDD-style Given-When-Then)
```java
// Java / JUnit 5
@DisplayName("When authenticated user requests agent list with detail=true, Then agents with state and ownership are returned")
@Test
void Given_UserIsAuthenticated_When_AgentListRequestedWithDetail_Then_ResponseIncludesStateAndOwnership() {
    // Arrange
    ...
    // Act
    ...
    // Assert
    ...
}
```

```typescript
// TypeScript / Vitest
it('Given an authenticated user, When the agent list endpoint is called with detail=true, Then the response includes agent state and ownership', () => {
  // Arrange
  ...
  // Act
  ...
  // Assert
  ...
});
```

#### Test Structure (AAA)
```java
@Test
void Given_..._When_..._Then_() {
    // --- Arrange --- (setup mocks, fixtures, test data)
    // --- Act ---     (execute the unit under test)
    // --- Assert ---  (verify expected outcome)
}
```

### Backend Test Strategy
- **Unit tests**: Use Mockito to mock `AgentManager`, `AgentContainer`, etc. Test handler/service classes in isolation.
- **Integration tests**: Use Vert.x `VertxUnit` / `WebTestClient` to test REST endpoints end-to-end against an in-memory JADE runtime (or a mock platform).
- **Test layout**: `src/test/java/...` following the same package structure as `src/main/java`.

### Frontend Test Strategy
- **Unit tests**: Vitest + React Testing Library. Mock `axios` calls. Test component rendering, error states, and prop handling.
- **Integration tests**: Test full API client → component data flow with mocked responses.
- **Test layout**: `apps/frontend/src/**/*.test.tsx`, `packages/shared/src/**/*.test.ts`.

### REST API Design Rules
1. RESTful resource naming (`/api/agents`, `/api/containers`)
2. HTTP methods map to CRUD: GET (read), POST (create), PUT (update), DELETE (delete), PATCH (partial)
3. Paginated responses for lists (`limit`, `offset` query params)
4. Consistent error response format: `{"error": "message", "code": 404}`
5. All endpoints return JSON with `Content-Type: application/json`
6. 2XX = success, 4XX = client error, 5XX = server error
7. Each endpoint documented with input/output JSON schema in `docs/api/`
