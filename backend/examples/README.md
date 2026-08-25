# Jade Examples

Example multi-agent scenarios for the Jade platform. Each subfolder is a
self-contained scenario you can start with the platform, deploy at runtime
through the REST API, and observe live in the React frontend.

## Scenarios

| Scenario | Folder | Description |
|----------|--------|-------------|
| Online shop | [`shop/`](shop/) | Customers buy from a storefront; the storefront reserves stock in the warehouse; the warehouse auto-restocks from a supplier. |

## Building

From the repo root (the reactor builds all backend modules):

```bash
cd backend && mvn package -DskipTests
```

Artifacts:

- `backend/jade/target/jade-<version>.jar` — platform uber jar
- `backend/examples/target/examples-<version>.jar` — example agents

## Running the online-shop scenario

### Option A: configuration file (all agents at startup)

```bash
java -cp backend/jade/target/jade-*.jar:backend/examples/target/examples-*.jar \
  io.donbee.jade.Boot -conf backend/examples/conf/shop.properties
```

### Option B: runtime deployment via REST

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
