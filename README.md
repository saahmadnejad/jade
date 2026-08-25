# Jade – Java Agent DEvelopment Framework (Modernized)

A fork of JADE (Java Agent DEvelopment Framework) running on Java 21 with virtual threads, bundled with a React + Vite frontend and Docker compose for local development.

## Project Structure

```
jade/
├── backend/                    # JADE framework (Java 21, multi-module Maven build)
│   ├── pom.xml                 # Parent POM (jade-parent) with CI-friendly ${revision} version
│   ├── fipa/                   # FIPA common library (CORBA-generated classes, FIPANames)
│   ├── jade/                   # Platform code (shade plugin -> uber jar backend.jar)
│   └── examples/               # Example scenarios (e.g. online shop) - see backend/examples/README.md
├── frontend/                   # React + Vite + TypeScript UI
│   ├── pnpm-workspace.yaml     # pnpm monorepo config
│   ├── package.json            # Root workspace package
│   ├── apps/webapp/            # React app (Vite dev server on :3000)
│   └── packages/shared/        # Shared TS library (API client for webapp + mobile)
├── docker/                     # Docker build files
│   ├── Dockerfile.backend      # Multi-stage: Maven -> JRE 21 Alpine
│   ├── Dockerfile.frontend     # Multi-stage: Node -> pnpm -> Vite build -> nginx
│   └── nginx.conf              # nginx config (SPA fallback + /api proxy incl. WebSocket)
├── docker-compose.yml          # Root compose file
└── .dockerignore / .gitignore
```

## Quick Start

```bash
# Build and start both containers
podman compose up --build -d

# Check status
podman compose ps

# Frontend: http://localhost:3000
# Backend (JADE RMI): localhost:10990
# Backend (REST API): http://localhost:8080/api
```

### Local Development

**Backend (JADE):**
```bash
cd backend && mvn compile exec:java -Dexec.mainClass="io.donbee.jade.Boot"
```

**Frontend (React):**
```bash
cd frontend && pnpm install && pnpm --filter webapp dev
# Vite dev server: http://localhost:3000
```

## Architecture

### Backend (`backend/`)

The backend is a fork of JADE under the `io.donbee.jade` package. Key components:

- **`io.donbee.jade.Boot`** — Main entry point. Parses command-line args and starts the JADE runtime.
- **`io.donbee.jade.core.Runtime`** — Singleton managing JADE container lifecycle.
- **`io.donbee.jade.core.Profile` / `ProfileImpl`** — Configuration properties for platform startup.
- **Virtual threads** — All threads use `Thread.ofVirtual()` (Java 21+).
- **REST API** — Built-in Vert.x REST server on port 8080. 38 endpoints covering platform info, containers, agents, tools, and remote platform management. Configurable via `-rest-port <n>`.

### Frontend (`frontend/`)

A pnpm monorepo with two packages:

- **`apps/webapp/`** — React 18 + Vite + TypeScript webapp. Uses Vitest + React Testing Library for tests.
- **`packages/shared/`** — Shared TypeScript library with `HttpClient` interface (DIP), typed API clients per domain (PlatformAPI, ContainerAPI, AgentAPI, ToolAPI, RemotePlatformAPI), and TypeScript types for all API responses. Shared between webapp and mobile apps.

### Docker

- **Backend**: Multi-stage build (Maven → JRE 21 Alpine). Runs `java -jar app.jar`.
- **Frontend**: Multi-stage build (Node → pnpm install → Vite build → nginx Alpine).
- **docker-compose.yml** — Both services on a single network; webapp depends on backend.

## Testing

**Backend:**
```bash
cd backend && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test
```

**Frontend (unit + integration):**
```bash
cd frontend
pnpm --filter shared test:run   # API client unit tests
pnpm --filter webapp test     # App integration tests
```
