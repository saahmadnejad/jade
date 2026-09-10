# Jade Examples

Example multi-agent scenarios for the Jade platform. Each scenario is
self-contained: start it from the platform, launch instances at runtime
through the REST API, and watch them live in the React frontend.

## Scenarios

| Scenario | Folder | Description |
|----------|--------|-------------|
| Dev team | `dev-team` (in `src/main/java/.../devteam/`) | Five LLM-powered agents (Manager, Architect, Implementer, Tester, Reviewer) build a small project from a brief, in bounded review rounds. |

## Building

From the repo root (the reactor builds all backend modules):

```bash
cd backend && mvn package -DskipTests
```

Artifacts:

- `backend/jade/target/jade-<version>.jar` — platform uber jar
- `backend/examples/target/examples-<version>.jar` — example agents

## Running the dev-team scenario

### Option A: Scenarios page in the UI (recommended)

Open the frontend, click the **Software Development Team** card, adjust the
config form (defaults prefilled) and press Start. Each launch creates its own
container (`scenario-<instance>`) so you can run several instances side by
side and kill them independently from the same page.

Watch the conversation on the **Messages** page (Manager task REQUESTs and
peer INFORMs, all FIPA `fipa-request` protocol). The team's files live in
`/tmp/jade-devteam-<instance>/` inside the backend container (see `AGENTS.md`
for the workspace layout). Review rounds iterate implement → test → review
until the Reviewer approves or `maxRounds` / `maxTotalCalls` caps hit; each
phase has its own time budget.

GitHub publishing is opt-in: set `githubOrg` to a non-empty value AND provide
a `GH_TOKEN` env var (PAT with repo/admin access) in the backend container.
With `githubOrg` empty (default) the team finishes at approval without
publishing.

Programmatically the same thing:

```bash
curl -s -X POST http://localhost:8080/api/scenarios/dev-team/instances \
  -H 'Content-Type: application/json' \
  -d '{"instanceName":"team-demo","config":{}}'

# list / stop
curl -s http://localhost:8080/api/scenarios/instances
curl -s -X DELETE http://localhost:8080/api/scenarios/instances/team-demo
```

The body's `config` object MUST nest under `"config"` (a flat body is silently
ignored → empty brief → FAILED instance).

### Option B: runtime deployment via REST

The scenario instances endpoint (above) deploys all five agents at once into a
fresh per-instance container. There is no per-agent deployment flow for the
dev-team scenario; the generic `/api/agents` endpoint remains available for
your own agents.

## API keys (LLM provider)

The dev-team role agents think through the `opencode` CLI, which reaches an
LLM provider configured per instance workspace (see `docs/adr/0003`).
**Keys are never stored in this repository** (see `docs/adr/0002`):

```bash
# Environment variable (passed through docker compose)
export TOKENROUTER_API_KEY=<your-tokenrouter-key>
```

The provider is defined in an `opencode.json` written into each instance
workspace at start; the API key is injected from the environment, never from
a file. Role agents fail fast with an actionable error when the CLI or the
key is missing.

## Using Jade as a library (no UI, no bundled examples)

The platform is a normal Maven artifact — the UI and the example scenarios are
optional extras:

```bash
cd backend && mvn install        # installs io.donbee:fipa, io.donbee:jade, io.donbee:examples
```

- **Maven dependency**: add `io.donbee:jade:<version>` to your project, write
  agents extending `io.donbee.jade.core.Agent`, run `io.donbee.jade.Boot`
- **Single jar**: take `backend/jade/target/jade-<version>.jar` (shaded, no UI,
  no examples) and start your agents:

  ```bash
  java -cp jade-<version>.jar:my-agents.jar io.donbee.jade.Boot -agents a1:com.me.MyAgent
  ```

- **Own scenarios in the UI**: implement
  `io.donbee.jade.rest.scenario.Scenario`, register it in your jar's
  `META-INF/services/io.donbee.jade.rest.scenario.Scenario`, drop the jar on
  the platform classpath — it appears on the Scenarios page automatically.

The Docker image bundles the examples for convenience; scenario code stays
inert until an instance is started through `/api/scenarios`.
