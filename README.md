# Jade – Java Agent DEvelopment Framework (Modernized)

A fork of JADE (Java Agent DEvelopment Framework) running on Java 21 with virtual threads, bundled with a React + Vite frontend and Docker compose for local development.

## Project Structure

```
jade/
├── backend/                    # JADE framework (Java 21, 1007 source files)
│   ├── pom.xml                 # Maven build (shade plugin -> uber jar)
│   └── src/main/java/io/donbee/jade/
├── frontend/                   # React + Vite + TypeScript UI
│   ├── pnpm-workspace.yaml     # pnpm monorepo config
│   ├── package.json            # Root workspace package
│   ├── apps/frontend/          # React app (Vite dev server on :3000)
│   └── packages/shared/        # Shared TS library (axios API client)
├── docker/                     # Docker build files
│   ├── Dockerfile.backend      # Multi-stage: Maven -> JRE 21 Alpine
│   ├── Dockerfile.frontend     # Multi-stage: Node -> pnpm -> Vite build -> nginx
│   └── nginx.conf              # nginx config (SPA fallback + /api proxy)
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
cd frontend && pnpm install && pnpm dev
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

- **`apps/frontend/`** — React 18 + Vite + TypeScript app. Uses Vitest + React Testing Library for tests.
- **`packages/shared/`** — Shared TypeScript library with `HttpClient` interface (DIP), typed API clients per domain (PlatformAPI, ContainerAPI, AgentAPI, ToolAPI, RemotePlatformAPI), and TypeScript types for all API responses.

### Docker

- **Backend**: Multi-stage build (Maven → JRE 21 Alpine). Runs `java -jar app.jar`.
- **Frontend**: Multi-stage build (Node → pnpm install → Vite build → nginx Alpine).
- **docker-compose.yml** — Both services on a single network; frontend depends on backend.

## Testing

**Backend:**
```bash
cd backend && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test
```

**Frontend (unit + integration):**
```bash
cd frontend
pnpm --filter shared test:run   # API client unit tests
pnpm --filter frontend test     # App integration tests
```
