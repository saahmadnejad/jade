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

- **Source root**: `backend/src/main/java/ir/donbee/jade/`
- **Java version**: 21 (virtual threads via `Thread.ofVirtual()`)
- **Build tool**: Maven (`pom.xml` at `backend/pom.xml`)
- **Main class**: `ir.donbee.jade.Boot` (declared in pom.xml shade plugin)
- **Dependencies**: JacORB 3.9, commons-codec 1.18.0
- **Package**: All code under `ir.donbee.jade.*`
- **No REST API**: JADE communicates via RMI/LEAP/JICP. To add HTTP endpoints to the React frontend, you must add a Java HTTP server or REST layer inside the backend (e.g. `java.net.http.HttpServer` or embedded Jetty).

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
1. Edit Java files in `backend/src/main/java/ir/donbee/jade/`
2. Test locally: `mvn compile exec:java -Dexec.mainClass="ir.donbee.jade.Boot"`
3. Rebuild Docker with `podman compose up --build -d`

## Important Files

- `backend/src/main/java/ir/donbee/jade/Boot.java` — Entry point, CLI arg parsing
- `backend/src/main/java/ir/donbee/jade/core/Profile.java` — Profile constants
- `backend/src/main/java/ir/donbee/jade/core/ProfileImpl.java` — Profile implementation
- `backend/src/main/java/ir/donbee/jade/core/FullResourceManager.java` — Virtual thread management
- `frontend/apps/frontend/src/App.tsx` — Root React component
- `frontend/packages/shared/src/api/client.ts` — Axios API client
- `docker/Dockerfile.backend` — Backend Docker build
- `docker/Dockerfile.frontend` — Frontend Docker build
