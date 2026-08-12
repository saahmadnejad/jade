# REST API Reference

Complete documentation for all REST API endpoints.

## Base URL

- **Local:** `http://localhost:8080`
- **Docker:** `http://localhost:8080` (mapped from container)
- **Configurable:** `-rest-port <n>` CLI argument

Default port: `8080`

---

## 1. Platform Operations

### GET /api/platform
Get platform metadata.

**Output (200):**
```json
{
  "platformID": "jade-main",
  "containerName": "Main-Container",
  "isMain": true,
  "ams": "ams@jade-main",
  "defaultDF": "df@jade-main"
}
```

### POST /api/platform/shutdown
Shutdown the platform.

**Input:**
```json
{"confirm": true}
```

**Output (200):**
```json
{"message": "Platform shutdown initiated"}
```

**Error (403):** `{"error": "Not a Main Container"}`

### GET /api/health
Health check.

**Output (200):**
```json
{"status": "ok"}
```

### GET /api/version
JADE version information.

**Output (200):**
```json
{"version": "string", "revision": "string", "date": "string"}
```

---

## 2. Container Operations

### GET /api/containers
List all containers.

**Output (200):**
```json
{
  "containers": [
    {
      "name": "Main-Container",
      "address": "127.0.0.1",
      "port": "1099",
      "isMain": true
    }
  ]
}
```

### GET /api/containers/{name}
Get a single container by name.

**Output (200):**
```json
{
  "name": "Main-Container",
  "address": "127.0.0.1",
  "port": "1099",
  "isMain": true
}
```

**Error (404):** `{"error": "Container not found: nonexistent"}`

### DELETE /api/containers/{name}
Kill a container.

**Output (200):**
```json
{"message": "Container 'Main-Container' killed"}
```

**Error (404):** `{"error": "Container not found"}`

### POST /api/containers/{name}/save
Save container state to a repository.

**Input:**
```json
{"repository": "file://./container-store"}
```

**Output (200):**
```json
{"message": "Container 'Main-Container' saved to file://./container-store"}
```

### POST /api/containers/{name}/load
Load container state from a repository.

**Input:**
```json
{"repository": "file://./container-store"}
```

**Output (200):**
```json
{"message": "Container 'Main-Container' loaded"}
```

### GET /api/containers/{name}/mtps
List MTPs installed on a container.

**Output (200):**
```json
{
  "mtps": [
    {"address": "127.0.0.1:1099", "className": "jade.mtp.tcl.TcpMTP"}
  ]
}
```

### POST /api/containers/{name}/mtps
Install a new MTP on a container.

**Input:**
```json
{
  "className": "jade.mtp.tcl.TcpMTP",
  "address": "127.0.0.1:1100"
}
```

**Output (200):**
```json
{"address": "127.0.0.1:1100", "className": "jade.mtp.tcl.TcpMTP"}
```

### DELETE /api/containers/{name}/mtps/{address}
Uninstall an MTP from a container.

**Output (200):**
```json
{"message": "MTP at '127.0.0.1:1100' uninstalled from 'Main-Container'"}
```

---

## 3. Agent Operations

### GET /api/agents
List agents (optionally filter by container or state).

**Query parameters:**
- `container` (optional) — filter by container name
- `state` (optional) — filter by state (ALL, ACTIVE, SUSPENDED, etc.)

**Output (200):**
```json
{
  "agents": [
    {
      "name": "rma@jade-main",
      "state": "ACTIVE",
      "ownership": "init",
      "container": "Main-Container",
      "addresses": ["jades://127.0.0.1:1099/jade-tools"]
    }
  ]
}
```

### GET /api/agents/{name}
Get details for a specific agent by local name.

**Output (200):**
```json
{
  "name": "rma@jade-main",
  "state": "ACTIVE",
  "ownership": "init",
  "container": "Main-Container",
  "addresses": ["jades://127.0.0.1:1099/jade-tools"]
}
```

**Error (404):** `{"error": "Agent not found: nonexistent"}`

### POST /api/agents
Deploy a new agent.

**Input:**
```json
{
  "name": "my-agent",
  "class": "com.example.MyAgent",
  "args": ["arg1", "arg2"]
}
```

**Output (201):**
```json
{"message": "Agent 'my-agent' created", "name": "my-agent@jade-main"}
```

**Error (400):** `{"error": "Both 'name' and 'class' are required"}`

**Error (409):** `{"error": "Agent name already exists"}`

### DELETE /api/agents/{name}
Kill an agent.

**Output (200):**
```json
{"message": "Agent 'rma' killed"}
```

**Error (404):** `{"error": "Agent not found: nonexistent"}`

### POST /api/agents/{name}/suspend
Suspend an agent.

**Output (200):**
```json
{"message": "Agent 'my-agent' suspended"}
```

### POST /api/agents/{name}/resume
Resume a suspended agent.

**Output (200):**
```json
{"message": "Agent 'my-agent' resumed"}
```

### POST /api/agents/{name}/freeze
Freeze an agent (save to buffer container).

**Input:**
```json
{
  "container": "Buffer-Container",
  "repository": "file://./agent-store"
}
```

**Output (200):**
```json
{"message": "Agent 'my-agent' frozen"}
```

### POST /api/agents/{name}/thaw
Thaw a frozen agent.

**Input:**
```json
{
  "container": "Main-Container",
  "repository": "file://./agent-store"
}
```

**Output (200):**
```json
{"message": "Agent 'my-agent' thawed"}
```

### POST /api/agents/clone
Clone an agent.

**Input:**
```json
{
  "name": "original-agent",
  "newName": "cloned-agent",
  "container": "Node1-Container"
}
```

**Output (201):**
```json
{"message": "Agent 'cloned-agent' cloned from 'original-agent'", "name": "cloned-agent@jade-main"}
```

### POST /api/agents/{name}/move
Move an agent to a different container.

**Input:**
```json
{
  "container": "Node1-Container"
}
```

**Output (200):**
```json
{"message": "Agent 'my-agent' moved to 'Node1-Container'"}
```

### POST /api/agents/{name}/save
Save agent state to a repository.

**Input:**
```json
{
  "repository": "file://./agent-store"
}
```

**Output (200):**
```json
{"message": "Agent 'my-agent' saved to 'file://./agent-store'"}
```

### POST /api/agents/load
Load an agent from a repository.

**Input:**
```json
{
  "name": "my-agent",
  "container": "Main-Container",
  "repository": "file://./agent-store"
}
```

**Output (200):**
```json
{"message": "Agent 'my-agent' loaded from 'file://./agent-store'"}
```

### PATCH /api/agents/{name}
Change an agent's ownership.

**Input:**
```json
{
  "ownership": "new-owner"
}
```

**Output (200):**
```json
{"message": "Agent 'my-agent' ownership changed to 'new-owner'"}
```

### POST /api/agents/register-remote
Register a remote agent with the local AMS.

**Input:**
```json
{
  "aid": "foreign-agent@foreign-platform",
  "addresses": ["jades://192.168.1.10:1200"]
}
```

**Output (200):**
```json
{"message": "Agent 'foreign-agent@foreign-platform' registered with local AMS"}
```

**Error (400):** `{"error": "Missing aid in request body"}`

---

## 4. Remote Platform Operations

### GET /api/platforms
List remote platforms.

**Output (200):**
```json
{
  "platforms": [
    {
      "name": "jade-main",
      "ams": "ams@jade-main",
      "addresses": ["127.0.0.1:1099"],
      "services": ["FIPAAgentManagement", "Mobility", "Messaging"]
    }
  ]
}
```

### POST /api/platforms
Add a remote platform via AMS AID.

**Input:**
```json
{
  "ams": "ams@remote-platform",
  "addresses": ["jades://192.168.1.10:1099"]
}
```

**Output (201):**
```json
{"message": "Platform 'remote-platform' added", "name": "remote-platform", "ams": "ams@remote-platform"}
```

**Error (400):** `{"error": "Missing ams endpoint or addresses"}`

### POST /api/platforms/fetch
Add a remote platform via URL.

**Input:**
```json
{
  "url": "host:1099"
}
```

**Output (201):**
```json
{"message": "Platform added", "name": "host:1099/FIPA", "ams": "host:1099/FIPA"}
```

**Error (400):** `{"error": "Missing url in request body"}`

### DELETE /api/platforms/{name}
Remove a remote platform.

**Output (200):**
```json
{"message": "Remote platform removed"}
```

**Error (501):** `{"error": "Not supported"}`

### GET /api/platforms/{name}/description
Get AP description for a remote platform.

**Output (200):**
```json
{
  "name": "jade-main",
  "ams": "ams@jade-main",
  "addresses": ["127.0.0.1:1099"],
  "services": ["FIPAAgentManagement", "Mobility", "Messaging"]
}
```

**Error (404):** `{"error": "Remote platform not found: unknown"}`

### POST /api/platforms/{name}/refresh
Refresh AP description for a remote platform.

**Output (200):**
```json
{
  "message": "Platform description refreshed",
  "name": "jade-main",
  "ams": "ams@jade-main",
  "addresses": ["127.0.0.1:1099"],
  "services": ["FIPAAgentManagement", "Mobility", "Messaging"]
}
```

### GET /api/platforms/{name}/agents
Search agents on a remote platform.

**Output (200):**
```json
{
  "agents": [
    {
      "name": "rma@remote-platform",
      "addresses": ["jades://192.168.1.10:1099/jade-tools"]
    }
  ]
}
```

**Error (400):** `{"error": "Missing platform name"}`

---

## 5. Tool Launch Operations

### POST /api/tools/{tool}/start
Start a GUI tool agent (sniffer, dummy, logger, introspector, df-gui).

**Path parameters:**
- `tool` — tool name: `sniffer`, `dummy`, `logger`, `introspector`, or `df-gui`

**Input:**
```json
{
  "container": "Main-Container"
}
```

**Output (201):**
```json
{"message": "Tool 'sniffer' started", "agent": "sniffer@jade-main"}
```

**Error (400):** `{"error": "Unknown tool: foobar"}`

**Error (500):** `{"error": "..."}` — tool classpath not available

---

## Common Errors

All errors return JSON:
```json
{"error": "error message", "code": 404}
```

HTTP Status Codes:
- `200` — Success
- `201` — Created
- `400` — Bad request (missing/invalid parameters)
- `403` — Forbidden (not a Main Container)
- `404` — Not found
- `409` — Conflict (agent name already exists)
- `500` — Internal server error
- `501` — Not implemented
