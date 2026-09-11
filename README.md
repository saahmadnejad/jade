# Jade – Java Agent DEvelopment Framework (Modernized)

[![CI](https://github.com/saahmadnejad/jade/actions/workflows/ci.yml/badge.svg)](https://github.com/saahmadnejad/jade/actions/workflows/ci.yml)
[![Real IT](https://github.com/saahmadnejad/jade/actions/workflows/real-it.yml/badge.svg)](https://github.com/saahmadnejad/jade/actions/workflows/real-it.yml)
[![Java 21](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![License: LGPL-2.1](https://img.shields.io/badge/License-LGPL--2.1-blue)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html)
[![JADE](https://img.shields.io/badge/JADE-jade.tilab.com-orange)](https://jade.tilab.com/)

A fork of JADE (Java Agent DEvelopment Framework) running on Java 21 with virtual threads, bundled with a React + Vite frontend and Docker compose for local development.

## Highlights

- **REST API** (~46 endpoints) + **React UI**: agents, containers, DF, tools, remote platforms, scenarios, live messages
- **Live message traffic** in the browser (WebSocket): watch FIPA ACL conversations as they happen (MessagesPage)
- **Scenarios page**: launch configurable multi-agent demo scenarios with one click; each instance runs in its own container and can be stopped independently — or add your own scenarios by dropping a jar implementing the `io.donbee.jade.rest.scenario.Scenario` SPI on the classpath
- **LLM-powered agents**: role agents think through the `opencode` CLI (bundled in the backend image) reaching any provider it supports — tokenrouter by default (see `docs/adr/0003`); the dev-team scenario's five AI agents build a small project from a brief
- **Library-ready artifacts**: `io.donbee:jade` (+ `fipa`, `llm`, `examples`) installable via Maven; the platform uber jar runs standalone without the UI

## Project Structure

```
jade/
├── backend/                    # JADE framework (Java 21, multi-module Maven build)
│   ├── pom.xml                 # Parent POM (jade-parent) with CI-friendly ${revision} version
│   ├── fipa/                   # FIPA common library (CORBA-generated classes, FIPANames)
│   ├── llm/                    # Framework-agnostic LLM client (OpenAI-compatible, SOCKS5 proxy)
│   ├── jade/                   # Platform code (shade plugin -> uber jar jade-<version>.jar)
│   └── examples/               # Example scenarios (dev team) - see backend/examples/README.md
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
# 1) LLM provider key (required for the dev-team scenario): the backend
#    reaches tokenrouter through the opencode CLI bundled in its image
#    (see docs/adr/0003). The key is injected from the environment:
export TOKENROUTER_API_KEY=<your-tokenrouter-key>

# 2) Build and start all containers (docker works identically)
podman compose up --build -d

# Check status
podman compose ps

# Frontend: http://localhost:3000
# Backend (JADE RMI): localhost:10990
# Backend (REST API): http://localhost:8080/api
```

### Local Development

**Backend (JADE)** (needs a full JDK 21 with `javac` on `JAVA_HOME`; distro `java-21-openjdk` is often JRE-only):
```bash
# Compile the reactor once (jade depends on fipa/llm), then run only the jade
# module — exec:java on the whole reactor fails (no Boot class in the parent):
cd backend && mvn -pl jade -am compile
cd backend && mvn -pl jade exec:java -Dexec.mainClass="io.donbee.jade.Boot"
# Ports clash with a running docker stack: use -rest-port 18080 -port 11997
# (see `Boot -help` for all options)
```

**Frontend (React):**
```bash
cd frontend && pnpm install && pnpm --filter webapp dev
# Vite dev server: http://localhost:3000
# (auto-moves to :3001 etc. if the port is busy, e.g. while the docker stack runs)
```

## Architecture

### Backend (`backend/`)

The backend is a fork of JADE under the `io.donbee.jade` package. Key components:

- **`io.donbee.jade.Boot`** — Main entry point. Parses command-line args and starts the JADE runtime.
- **`io.donbee.jade.core.Runtime`** — Singleton managing JADE container lifecycle.
- **`io.donbee.jade.core.Profile` / `ProfileImpl`** — Configuration properties for platform startup.
- **Virtual threads** — All threads use `Thread.ofVirtual()` (Java 21+).
- **REST API** — Built-in Vert.x REST server on port 8080. ~46 endpoints covering platform info, containers, agents, tools, remote platforms, scenarios, DF, and live message traffic. Configurable via `-rest-port <n>`.

### Frontend (`frontend/`)

A pnpm monorepo with two packages:

- **`apps/webapp/`** — React 18 + Vite + TypeScript webapp. Uses Vitest + React Testing Library for tests.
- **`packages/shared/`** — Shared TypeScript library with `HttpClient` interface (DIP), typed per-domain API clients (`platform`, `containers`, `agents`, `tools`, `platforms`, `df`, `messages`, `scenarios`), and a WebSocket subscription for live ACL traffic. Shared between webapp and mobile apps.

### Docker

- **Backend**: Multi-stage build (Maven → JRE 21 Alpine). Runs `java -cp app.jar:examples.jar:llm.jar io.donbee.jade.Boot`.
- **Frontend**: Multi-stage build (Node → pnpm install → Vite build → nginx Alpine).
- **docker-compose.yml** — Two services (backend, frontend) on a single network; frontend depends on backend. LLM access goes through the `opencode` CLI inside the backend image (no gateway container).

## Testing

**Backend:**
```bash
cd backend && mvn test
```

**Frontend (unit + integration):**
```bash
cd frontend
pnpm --filter shared test:run   # API client unit tests
pnpm --filter webapp test:run    # App integration tests (non-watch; `test` runs in watch mode)
```

**Dev-team scenario:**
```bash
# Offline flow test (stubbed CLI, no network) — runs in the default suite:
cd backend && mvn -pl examples -am test -Dtest=DevTeamFlowIntegrationTest

# Real run against tokenrouter (costs tokens; needs network + key):
TOKENROUTER_API_KEY=... cd backend && \
  mvn -pl examples -am test -Dtest=DevTeamRealIT -Dit.real=true -Dsurefire.failIfNoSpecifiedTests=false
```
