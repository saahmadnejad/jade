# Jade Examples

Example multi-agent scenarios for the Jade platform. Each subfolder is a
self-contained scenario you can start with the platform, deploy at runtime
through the REST API, and observe live in the React frontend.

## Scenarios

| Scenario | Folder | Description |
|----------|--------|-------------|
| Online shop | [`shop/`](shop/) | Customers buy from a storefront; the storefront reserves stock in the warehouse; the warehouse auto-restocks from a supplier. |
| Dev team | `dev-team` (in `src/main/java/.../devteam/`) | Five LLM-powered agents (Manager, Architect, Implementer, Tester, Reviewer) build a small project from a brief, in bounded review rounds. |

## API keys (LLM scenarios)

The dev-team scenario calls LLM providers and needs an API key at runtime.
**Keys are never stored in this repository** (see `docs/adr/0002`):

```bash
# Option 1: environment variable
export OPENROUTER_API_KEY=sk-or-v1-...

# Option 2: gitignored local file
echo "api.key=sk-or-v1-..." > backend/examples/conf/secrets.local.properties
```

Without a key the role agents fail fast with an actionable error.

By default LLM traffic routes through a SOCKS5 proxy (`192.168.1.151:10808`,
configurable per instance on the Scenarios page). Any OpenAI-compatible
provider works by changing `baseUrl` + model: paid OpenRouter tiers, OpenAI,
DeepSeek direct, or a local Ollama (`http://localhost:11434/v1`, no key).

## Building

From the repo root (the reactor builds all backend modules):

```bash
cd backend && mvn package -DskipTests
```

Artifacts:

- `backend/jade/target/jade-<version>.jar` — platform uber jar
- `backend/examples/target/examples-<version>.jar` — example agents

## Running the online-shop scenario

### Option A: Scenarios page in the UI (recommended)

Open the frontend, click a scenario card (**Online Shop** or **Software
Development Team**), adjust the config form (defaults prefilled) and press
Start. Each launch creates its own container (`scenario-<instance>`) so you can
run several instances side by side and kill them independently from the same
page.

For the dev team: watch the conversation on the **Messages** page; when the
instance finishes you'll find the produced project (`BRIEF.md`, `DESIGN.md`,
`src/…`, `tests/…`, review reports) mirrored to disk if you set a workspace
directory — otherwise ask the team's artifacts from the instance logs.

Programmatically the same thing:

```bash
curl -s -X POST http://localhost:8080/api/scenarios/online-shop/instances \
  -H 'Content-Type: application/json' \
  -d '{"instanceName":"shop-demo","config":{"initialStock":25,"customerCount":3}}'

# list / stop
curl -s http://localhost:8080/api/scenarios/instances
curl -s -X DELETE http://localhost:8080/api/scenarios/instances/shop-demo
```

### Option B: configuration file (all agents at startup)

```bash
java -cp backend/jade/target/jade-*.jar:backend/examples/target/examples-*.jar \
  io.donbee.jade.Boot -conf backend/examples/conf/shop.properties
```

### Option C: runtime deployment via REST

Start an empty platform first, then deploy agents one by one:

```bash
# Deploy in this order so discovery works immediately
curl -s -X POST http://localhost:8080/api/agents -H 'Content-Type: application/json' \
  -d @backend/examples/rest/shop.json
curl -s -X POST http://localhost:8080/api/agents -H 'Content-Type: application/json' \
  -d @backend/examples/rest/inventory.json
curl -s -X POST http://localhost:8080/api/agents -H 'Content-Type: application/json' \
  -d @backend/examples/rest/supplier.json
curl -s -X POST http://localhost:8080/api/agents -H 'Content-Type: application/json' \
  -d @backend/examples/rest/customer1.json
```

## Watching it live

1. Open the frontend at `http://localhost:3000`
2. **Messages page** — watch the FIPA-REQUEST conversation flow between
   customers, shop, inventory and supplier in real time (WebSocket)
3. **DF page** — see the services each agent registered (`shop`,
   `inventory`, `supplier`)
4. **Agents page** — deploy more customers at runtime and watch traffic pick up

## Scenario walkthrough

```
customer1 ──(buy sku-phone 1)──▶ shop ──(reserve sku-phone 1)──▶ inventory
customer1 ◀─(order-confirmed)── shop ◀─(reserved sku-phone 1)── inventory

inventory (ticker: stock <= threshold) ──(restock sku-phone 20)──▶ supplier
inventory ◀─(restocked sku-phone 20)───────────────────────────── supplier
```

All messages use the FIPA-REQUEST interaction protocol; service lookup goes
through the Directory Facilitator. See each agent's JavaDoc for its role,
content language and configurable arguments.

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
