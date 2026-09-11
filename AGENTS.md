# AGENTS.md — Jade

## Build & Run

```bash
# Backend (Java 21; full JDK required — see JDK gotcha)
cd backend && mvn package -DskipTests
cd backend && mvn test                         # full suite across all modules
cd backend && mvn test -pl llm -am             # single module (-am builds deps)
cd backend && mvn -pl jade -am compile && mvn -pl jade exec:java -Dexec.mainClass="io.donbee.jade.Boot"

# Frontend
cd frontend && pnpm --filter webapp dev        # Vite, http://localhost:3000
pnpm --filter shared test:run                   # non-watch
pnpm --filter webapp test:run
pnpm --filter webapp lint

# Docker (compose at repo root; TOKENROUTER_API_KEY required or dev-team role agents fail)
export TOKENROUTER_API_KEY=<key>
docker compose up -d --build                   # podman compose works identically
```

**JDK gotcha**: distro `java-21-openjdk` is often JRE-only (no `javac`) and breaks `mvn` with cryptic errors. `JAVA_HOME` must point at a full JDK 21 that has `bin/javac` (SDKMAN JDKs qualify).

**Gotchas**
- Root `package.json` `dev`/`build` run `pnpm --filter frontend dev` — but "frontend" matches no workspace package, so it does nothing. Always `--filter webapp` or `--filter shared`.
- `pnpm --filter webapp test` runs in watch mode (hangs an agent). Use `test:run`.
- Running the backend locally: do **not** use plain `mvn compile exec:java` from `backend/` — `exec:java` runs in every reactor module and fails with `ClassNotFoundException: Boot` on the parent (no Boot there). Compile the reactor once (`-pl jade -am compile`), then run `exec:java` on `-pl jade` only. Ports 8080/1099 clash with the docker stack — pass `-rest-port`/`-port` overrides.
- Frontend tests mock `shared/api/factory`; add new API methods to the mock or tests crash.
- Docker Hub pulls occasionally 5xx/timeout — retry; base images cache after first pull.
- `docker compose up` without `--build` reuses the stale `jade-backend:latest` image even after `mvn package`. Source changes need `up -d --build` (or `docker compose build backend` first).
- `useFeedback()` in `frontend/.../NotificationSnackbar.tsx` must keep `notify`/`close` as stable `useCallback`s — unstable identities caused fetch loops spamming the scenarios/instances API.
- Dev-team tests: `DevTeamFlowIntegrationTest` (offline, stub CLI, default suite) needs `-am` — surefire otherwise resolves a stale `~/.m2` llm jar and agents die with `NoClassDefFoundError`. `DevTeamRealIT` needs `-Dit.real=true` + `TOKENROUTER_API_KEY` (real tokens).

## Architecture

### Backend (`io.donbee.jade.*`, Maven multi-module: `fipa`, `llm`, `jade`, `examples`)
- **Entry**: `io.donbee.jade.Boot` (CLI args → `ProfileImpl` → `Runtime`). `-conf` loads a properties file; `-name/-container/-host/-port/-rest-port` override defaults.
- **REST API**: Vert.x 5.1.6 on :8080 (configurable `-rest-port`), Main Container only. `RestAPIVerticle` only configures the router (SRP); ~46 routes under `/api/*`. Paths in `ApiRoutes`; one dedicated class per route group in `rest/handler/`.
- **Blocking rule**: handlers that do blocking work (LLM probes, container create/kill, synchronized service calls) MUST wrap it in `ctx.vertx().executeBlocking(...)` — the event loop froze 130s+ before this rule existed.
- **Main Container** = AMS + DF + REST API + all UI/REST-deployed agents. Each scenario instance gets its own container `scenario-<name>`.
- **Message capture**: `MessageTrafficMonitor` (`io.donbee.jade.core.messaging`) is a static JVM-wide registry; `MessagingService.CommandSourceSink` calls `MessageTrafficMonitor.notifyMessage(...)` for every dispatched ACL message across ALL in-process containers. Streamed via `WS /api/messages/stream`.
- **Scenario SPI**: any jar with `META-INF/services/io.donbee.jade.rest.scenario.Scenario` appears in `/api/scenarios`.
- **LLM brains** (`backend/llm`): `Brain` (interface), `CliBrain` (shells `opencode run --auto --agent <role> -m <model>`; used by dev-team per ADR-0003; `opencode.cli` system property / `OPENCODE_CLI` env override the binary — test seam), `RetryingBrain` (4 attempts = 1 + 3 retries, 5/15/45s gaps), `FallbackBrain` (chains on `BrainException` when a distinct fallbackModel is configured; empty default = single brain), plus the kept lib stack `LangChain4jBrain`+`BashTool` (langchain4j 0.35; unused by dev-team).
- All agent/platform threads are virtual (`Thread.ofVirtual()`). `kill -3` thread dumps show carrier frames only, not virtual-thread stacks.
- Deps: JacORB 3.9, commons-codec 1.18.0, Vert.x 5.1.6, langchain4j 0.35.0.

### Frontend (`frontend/`, pnpm 9 monorepo: `apps/webapp` + `packages/shared`)
- Stack: React 18 + TS + Vite 5 + MUI 9.x (dark) + ESLint strict. MUI deps are declared at `frontend/` root, not in `apps/webapp/package.json`.
- `shared` is a TS lib exposing one injected `api` client (built in `packages/shared/src/api/factory.ts`) with typed per-domain clients: `platform, containers, agents, tools, platforms, df, messages, scenarios` over a swappable `HttpClient` (axios). `subscribeMessagesStream()` (WebSocket, auto-reconnect) for live ACL traffic.
- SPA; nginx serves `index.html` fallback and proxies `/api/` to `backend:8080`. `/api/messages/stream` is proxied with WebSocket `Upgrade` headers; `docker/nginx.conf.template` sets `NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1` so nginx re-resolves `backend` per request (otherwise the backend IP is cached at startup and breaks after backend container recreation).

### Docker
- **Backend image**: multi-stage (Maven → `eclipse-temurin:21-jre-alpine`). `CMD ["java","-cp","app.jar:examples.jar:llm.jar","io.donbee.jade.Boot","-host","localhost","-local-host","localhost"]` (note the triple-classpath `-cp` — README's `java -jar app.jar` is incomplete). Includes `gh`, `opencode`, python3/node for agent tooling.
- **Ports**: backend RMI 1099→10990, REST 8080→8080; frontend nginx 80→3000. (No 9router service anymore — LLM access is the bundled `opencode` CLI inside the backend image; provider config is a workspace `opencode.json` per instance.)

### Dev-team scenario (`backend/examples/.../devteam/`)
- 5 agents: Manager (deterministic state machine, no LLM) → Architect → Implementer → Tester → Reviewer. Manager phases: CLARIFY→DESIGN→IMPLEMENT→TEST→REVIEW→PUBLISH with per-phase deadlines (`enterPhase()`) + 60s `TickerBehaviour` watchdog.
- Start body: `{"instanceName":"...","config":{...}}` — config MUST nest under `"config"`; a flat body is silently ignored → empty brief → instance FAILED.
- Agent workspace: `/tmp/jade-devteam-<instance>/` inside the backend container.
- `*Model` params are `provider/model` ids resolved by the opencode CLI (default `tokenrouter/z-ai/glm-5.3-free`). A model the CLI/auth can't serve fails at the first role-agent call → STATUS.md FAILED with reason.
- Peer messages use `PEER_PROTOCOL` = `FIPANames.InteractionProtocol.FIPA_REQUEST` (exact `fipa-request` token, case-sensitive — never hand-write it). Manager filters its own `dt-*` conversation IDs and ignores `dt-peer-*`.
- `githubOrg` defaults empty (memory-only, skips publish). Publishing needs non-empty `githubOrg` + `GH_TOKEN` env in the backend container.

## Secrets (ADR-0002 — never commit)
- GitHub publish token: `GH_TOKEN` env or gitignored `backend/examples/conf/secrets-local.properties` (`github.token`; template `secrets-local.properties.example`). `SecretsResolver` resolves only this now.
- LLM provider key: `TOKENROUTER_API_KEY` env, injected into the workspace `opencode.json` (`{env:TOKENROUTER_API_KEY}`) — never on disk (ADR-0003).
- `.gitignore` covers `secrets*.properties` + `secrets-*.properties`.

## Key files
- `backend/jade/src/main/java/io/donbee/jade/Boot.java` — CLI entry
- `backend/jade/src/main/java/io/donbee/jade/core/Profile*.java` — config constants
- `backend/jade/src/main/java/io/donbee/jade/rest/RestAPIVerticle.java` — route wiring
- `backend/jade/src/main/java/io/donbee/jade/rest/ApiRoutes.java` — route path constants
- `backend/jade/src/main/java/io/donbee/jade/rest/handler/` — one class per route group
- `backend/llm/src/main/java/io/donbee/llm/{Brain,LangChain4jBrain,BashTool,FallbackBrain}.java`
- `backend/examples/.../devteam/` — dev-team scenario
- `frontend/packages/shared/src/api/factory.ts` — `api` client builder
- `backend/examples/README.md`, `docs/api/` — scenario + REST schema docs

## Standards
- **Tests** (mandatory for new code): Arrange-Act-Assert, Given-When-Then naming. Backend: `src/test/java` mirrors, Mockito-mocked handlers, `RestAPIIntegrationTest` for endpoints. Frontend: `apps/webapp/src/**/*.test.tsx`, `packages/shared/src/**/*.test.ts`; mock axios/API factory; cover success/error/disabled/validation states.
- **REST rules**: resource naming (`/api/agents`, `/api/containers`); verbs = CRUD; errors `{"error":"...","code":<status>}` via global failure handler; every endpoint has input/output schema in `docs/api/`. Update the doc spec first, match code before committing.
- **REST handler JavaDoc**: every `rest/handler` class carries a `<b>Old GUI implementation</b>` JavaDoc section (old Swing class+method, FIPA protocol/ontology, delegated service method; "No direct Swing GUI equivalent" when none).
- **Docs hygiene**: never commit host-specific values (absolute/home paths, JDK paths, hostnames, usernames, real keys/tokens, ephemeral model names). Use repo-relative paths, env-var names, and "verify via `/v1/models`".

## Dev workflow (docs present)
- Issues: GitHub (saahmadnejad/jade) via `gh` CLI — `docs/agents/issue-tracker.md`.
- Triage labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`) — `docs/agents/triage-labels.md`.
- Domain context: `CONTEXT.md` + `docs/adr/` — `docs/agents/domain.md`.
