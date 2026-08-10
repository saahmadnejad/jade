# REST API Documentation

**Purpose:** Quick reference for implemented REST endpoints. For detailed specifications including JADE ontology backend calls and full schemas, see individual API spec files:
- Core platform/agents/containers → [rma-api.md](rma-api.md)
- Shared components → [shared-components-api.md](shared-components-api.md)

## Endpoints Not Covered in Other Docs

These endpoints are implemented but not specified in other API docs:

### GET /api/health
Health check endpoint.

**Response: 200 OK**
```json
{"status": "ok"}
```

### GET /api/version
Returns JADE version information.

**Response: 200 OK**
```json
{"version": "string", "revision": "string", "date": "string"}
```

### GET /api/agents/:name
Returns details for a specific agent by local name.

**Path Parameters:**
- `name` — agent local name (e.g., `df`)

**Response: 200 OK**
```json
{
  "name": "df@host:1099/JADE",
  "state": "active",
  "ownership": "NONE",
  "container": "Main-Container",
  "addresses": ["http://host:7778/acc"]
}
```

### GET /api/containers/:name
Returns details for a specific container by name.

**Path Parameters:**
- `name` — container name (e.g., `Main-Container`)

**Response: 200 OK**
```json
{
  "name": "Main-Container",
  "address": "127.0.0.1",
  "port": "1099",
  "isMain": true
}
```

## Endpoints Covered in rma-api.md

The following endpoints are fully specified in [rma-api.md](rma-api.md):

| Endpoint | Status |
|----------|--------|
| GET /api/platform | Specified & Implemented |
| POST /api/platform/shutdown | Specified & Implemented |
| GET /api/containers | Specified & Implemented |
| GET /api/agents | Specified & Implemented |
| POST /api/agents | Specified & Implemented *(see implementation notes)* |
| DELETE /api/agents/:name | Specified & Implemented |
| POST /api/agents/:name/suspend | Specified & Implemented |
| POST /api/agents/:name/resume | Specified & Implemented |

**Implementation note for POST /api/agents:** The implemented request body uses `class` (not `className`) and `args` (not `arguments`), both optional `container`/`owner` fields are omitted for simplicity.

## Error Format

All errors return JSON:
```json
{"error": "error message", "code": 404}
```

HTTP Status Codes:
- `200` — Success
- `201` — Created (agent deployed)
- `400` — Bad request (missing/invalid parameters)
- `403` — Forbidden (not a Main Container)
- `404` — Not found (agent/container doesn't exist)
- `409` — Conflict (agent name already exists)
- `500` — Internal server error
