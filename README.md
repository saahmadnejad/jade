# Jade – Java Agent DEvelopment Framework (Modernized)

A fork of JADE (Java Agent DEvelopment Framework) running on Java 21 with virtual threads, bundled with a React + Vite frontend and Docker compose for local development.

## Highlights

- **REST API** (~44 endpoints) + **React UI**: agents, containers, DF, tools, remote platforms
- **Live message traffic** in the browser (WebSocket): watch FIPA ACL conversations as they happen (MessagesPage)
- **Scenarios page**: launch configurable multi-agent demo scenarios with one click; each instance runs in its own container and can be stopped independently — or add your own scenarios by dropping a jar implementing the `io.donbee.jade.rest.scenario.Scenario` SPI on the classpath
- **LLM-powered agents**: `io.donbee:llm` speaks any OpenAI-compatible endpoint (9router by default, paid providers or local Ollama via config) via langchain4j; the dev-team scenario has five AI agents build a small project from a brief
- **Library-ready artifacts**: `io.donbee:jade` (+ `fipa`, `llm`, `examples`) installable via Maven; the platform uber jar runs standalone without the UI

## Project Structure

```
jade/
├── backend/                    # JADE framework (Java 21, multi-module Maven build)
│   ├── pom.xml                 # Parent POM (jade-parent) with CI-friendly ${revision} version
│   ├── fipa/                   # FIPA common library (CORBA-generated classes, FIPANames)
│   ├── llm/                    # Framework-agnostic LLM client (OpenAI-compatible, SOCKS5 proxy)
│   ├── jade/                   # Platform code (shade plugin -> uber jar jade-<version>.jar)
│   └── examples/               # Example scenarios (online shop, dev team) - see backend/examples/README.md
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
# 1) Configure your LLM provider key for 9router (one-time):
#    open http://localhost:20129/dashboard after first `up`, or run 9router on host.
#    Then export it for the backend container (required for LLM scenarios):
export NINEROUTER_API_KEY=$(grep 'llm.api.key' backend/examples/conf/secrets-local.properties | cut -d= -f2)
#    (create that gitignored file from backend/examples/conf/secrets-local.properties.example)

# 2) Build and start all containers (docker works identically)
podman compose up --build -d

# Check status
podman compose ps

# Frontend: http://localhost:3000
# Backend (JADE RMI): localhost:10990
# Backend (REST API): http://localhost:8080/api
# 9router dashboard: http://localhost:20129/dashboard
```

### Local Development

**Backend (JADE)** (needs a full JDK 21 with `javac` on `JAVA_HOME`; distro `java-21-openjdk` is often JRE-only):
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
cd backend && mvn test
```

**Frontend (unit + integration):**
```bash
cd frontend
pnpm --filter shared test:run   # API client unit tests
pnpm --filter webapp test:run    # App integration tests (non-watch; `test` runs in watch mode)
```
