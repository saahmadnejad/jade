# Jade – Java Agent DEvelopment Framework (Modernized)

A fork of JADE (Java Agent DEvelopment Framework) running on Java 21 with virtual threads, bundled with a React + Vite frontend and Docker compose for local development.

## Project Structure

```
jade/
├── backend/                    # JADE framework (Java 21, 1015 source files)
│   ├── pom.xml                 # Maven build (shade plugin -> uber jar)
│   └── src/main/java/ir/donbee/jade/
├── frontend/                   # React + Vite + TypeScript UI
│   ├── pnpm-workspace.yaml     # pnpm monorepo config
│   ├── package.json            # Root workspace package
│   ├── apps/frontend/          # React app (Vite dev server on :3000)
│   └── packages/shared/        # Shared TS library (axios API client)
├── docker/                     # Docker build files
│   ├── Dockerfile.backend      # Multi-stage: Maven -> JRE 21 Alpine
│   ├── Dockerfile.frontend     # Multi-stage: Node -> pnpm -> Vite -> nginx
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
```

### Local Development

**Backend (JADE):**
```bash
cd backend && mvn compile exec:java -Dexec.mainClass="ir.donbee.jade.Boot"
```

**Frontend (React):**
```bash
cd frontend && pnpm install && pnpm dev
# Vite dev server: http://localhost:3000
```

## Architecture

### Backend (`backend/`)

The backend is a fork of JADE under the `ir.donbee.jade` package. Key components:

- **`ir.donbee.jade.Boot`** — Main entry point. Parses command-line args and starts the JADE runtime.
- **`ir.donbee.jade.core.Runtime`** — Singleton managing JADE container lifecycle.
- **`ir.donbee.jade.core.Profile` / `ProfileImpl`** — Configuration properties for platform startup.
- **Virtual threads** — All threads use `Thread.ofVirtual()` (Java 21+).

JADE exposes its API via RMI (port 1099) using the LEAP/JICP protocol. No REST/HTTP endpoints exist yet — this is a planned enhancement.

### Frontend (`frontend/`)

A pnpm monorepo with two packages:

- **`apps/frontend/`** — React 18 + Vite + TypeScript app. Served by nginx in Docker.
- **`packages/shared/`** — Shared TypeScript library with an axios-based API client.

### Docker

- **Backend**: Multi-stage build (Maven → JRE 21 Alpine). Runs `java -jar app.jar`.
- **Frontend**: Multi-stage build (Node → pnpm install → Vite build → nginx Alpine).
- **docker-compose.yml** — Both services on a single network; frontend depends on backend.
