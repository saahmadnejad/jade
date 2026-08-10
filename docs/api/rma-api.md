# RMA REST API Specification

**Purpose:** This document is the canonical specification for core JADE platform REST endpoints (platform, containers, agents). It serves as the work queue for implementing the backend REST API — each endpoint's "JADE backend call" notes describe the JADE ontology/backend methods that need to be wired in. This doc should be updated as endpoints are implemented or new ones are planned.

Detailed input/output specifications for RMA (Remote Management Agent) functionalities. Each entry describes the REST endpoint design, request/response schemas, and the underlying JADE management ontology calls.

---

## 1. Platform Operations

### 1.0 Health Check
- **Endpoint**: `GET /api/health`
- **Input**: none
- **Output (200)**: `{"status": "ok"}`
- **JADE backend call**: None — static response

### 1.0b Version Info
- **Endpoint**: `GET /api/version`
- **Input**: none
- **Output (200)**:
```json
{"version": "string", "revision": "string", "date": "string"}
```
- **JADE backend call**: `io.donbee.jade.util.VersionManager` / `io.donbee.jade.Version`

### 1.1 Platform Info
- **Endpoint**: `GET /api/platform`
- **Input**: none
- **Output (200)**:
```json
{
  "platformID": "jade-main",
  "containerName": "Main-Container",
  "isMain": true,
  "ams": "ams@jade-main",
  "defaultDF": "df@jade-main"
}
```
- **Error (500)**: `{"error": "Failed to retrieve platform info"}`
- **JADE backend call**: `impl.getPlatformID()`, `impl.here().getName()`, `impl.getAMS().getName()`, `impl.getDefaultDF().getName()`

### 1.3 Shutdown Platform
- **Endpoint**: `POST /api/platform/shutdown`
- **Input**: 
```json
{
  "confirm": true
}
```
- **Output (200)**: `{"message": "Platform shutdown initiated"}`
- **Error (403)**: `{"error": "Not a Main Container"}` — only main container can shut down
- **Error (500)**: `{"error": "..."}`
- **JADE backend call**: AMS `ShutdownPlatform` action via `JADEManagementOntology`

---

## 2. Container Operations

### 2.1 List Containers
- **Endpoint**: `GET /api/containers`
- **Input**: none
- **Output (200)**:
```json
{
  "containers": [
    {
      "name": "Main-Container",
      "addresses": ["127.0.0.1:1099", "192.168.1.5:1099"],
      "isMain": true,
      "agentCount": 5,
      "mtps": ["jade.mtp.tcl.Tcp MTP"]
    },
    {
      "name": "Node1-Container",
      "addresses": ["127.0.0.1:1098"],
      "isMain": false,
      "agentCount": 3,
      "mtps": []
    }
  ]
}
```
- **JADE backend call**: Subscribe to AMS introspection events (AddedContainer, RemovedContainer)

### 2.1b Get Single Container
- **Endpoint**: `GET /api/containers/{name}`
- **Input**: path param `name` (container name, e.g. `Main-Container`)
- **Output (200)**:
```json
{
  "name": "Main-Container",
  "address": "127.0.0.1",
  "port": "1099",
  "isMain": true
}
```
- **Error (404)**: `{"error": "Container not found: nonexistent"}`
- **Error (403)**: `{"error": "Not a Main Container"}`
- **JADE backend call**: `agentManager.containerIDs()` to iterate, match by name

### 2.2 Kill Container
- **Endpoint**: `DELETE /api/containers/{name}`
- **Input**: path parameter `name` (container name), query param `confirm=true`
- **Output (200)**: `{"message": "Container 'Main-Container' killed"}`
- **Error (404)**: `{"error": "Container not found"}`
- **Error (403)**: `{"error": "Cannot kill Main Container"}`
- **JADE backend call**: AMS `KillContainer` action via `JADEManagementOntology`

### 2.3 Save Container
- **Endpoint**: `POST /api/containers/{name}/save`
- **Input**:
```json
{
  "repository": "file://./container-store"
}
```
- **Output (200)**: `{"message": "Container 'Main-Container' saved to file://./container-store"}`
- **Error (404)**: `{"error": "Container not found"}`
- **Error (500)**: `{"error": "Save failed"}`
- **JADE backend call**: `SaveContainer` action via `PersistenceOntology`

### 2.4 Load Container
- **Endpoint**: `POST /api/containers/{name}/load`
- **Input**:
```json
{
  "repository": "file://./container-store"
}
```
- **Output (200)**: `{"message": "Container 'Main-Container' loaded"}`
- **Error (404)**: `{"error": "Container not found or repository does not exist"}`
- **JADE backend call**: `LoadContainer` action via `PersistenceOntology`

### 2.5 Install MTP
- **Endpoint**: `POST /api/containers/{name}/mtps`
- **Input**:
```json
{
  "className": "jade.mtp.tcl.TcpMTP$0",
  "address": "127.0.0.1:1100"
}
```
- **Output (200)**: `{"message": "MTP installed on container 'Main-Container'", "address": "127.0.0.1:1100"}`
- **Error (404)**: `{"error": "Container not found"}`
- **JADE backend call**: AMS `InstallMTP` action via `JADEManagementOntology`

### 2.6 Uninstall MTP
- **Endpoint**: `DELETE /api/containers/{name}/mtps/{address}`
- **Input**: path params `name`, `address`
- **Output (200)**: `{"message": "MTP at '127.0.0.1:1100' uninstalled from 'Main-Container'"}`
- **Error (404)**: `{"error": "Container or MTP not found"}`
- **JADE backend call**: AMS `UninstallMTP` action via `JADEManagementOntology`

### 2.7 Manage MTPs (list + install/uninstall)
- **Endpoint**: `GET /api/containers/{name}/mtps`
- **Input**: none
- **Output (200)**:
```json
{
  "mtps": [
    {"address": "127.0.0.1:1099", "className": "jade.mtp.tcl.TcpMTP"},
    {"address": "jade@127.0.0.1:1100", "className": "jade.mtp.http.HTTP MTP"}
  ]
}
```
- **JADE backend call**: `impl.getAddresses()` for the container

---

## 3. Agent Operations

### 3.1 List Agents
- **Endpoint**: `GET /api/agents?container={name}&state={state}`
- **Input**: optional query params `container` (filter by container), `state` (ALL|ACTIVE|SUSPENDED|THAWED|FREEZEN)
- **Output (200)**:
```json
{
  "agents": [
    {
      "name": "rma@jade-main",
      "class": "io.donbee.jade.tools.rma.rma",
      "state": "ACTIVE",
      "ownership": "init",
      "container": "Main-Container",
      "addresses": ["jades://127.0.0.1:1099/jade-tools"]
    },
    {
      "name": "ams@jade-main",
      "class": "io.donbee.jade.domain.ams",
      "state": "ACTIVE",
      "ownership": "init",
      "container": "Main-Container",
      "addresses": []
    }
  ]
}
```
- **JADE backend call**: `agentManager.containerAgents(cid)` or AMS `AMSService` search

### 3.1b Get Single Agent
- **Endpoint**: `GET /api/agents/{name}`
- **Input**: path param `name` (agent local name, e.g. `rma`)
- **Output (200)**:
```json
{
  "name": "rma@jade-main",
  "state": "ACTIVE",
  "ownership": "init",
  "container": "Main-Container",
  "addresses": ["jades://127.0.0.1:1099/jade-tools"]
}
```
- **Error (404)**: `{"error": "Agent not found: nonexistent"}`
- **Error (403)**: `{"error": "Not a Main Container"}`
- **JADE backend call**: `agentManager.containerAgents(cid)` to iterate, match by local name or GUID

### 3.2 Start New Agent
- **Endpoint**: `POST /api/agents`
- **Input**:
```json
{
  "name": "my-agent",
  "className": "com.example.MyAgent",
  "container": "Main-Container",
  "owner": "init",
  "arguments": ["arg1", "arg2"]
}
```
- **Output (201)**: `{"message": "Agent 'my-agent' created", "aid": "my-agent@jade-main"}`
- **Error (400)**: `{"error": "Agent name already exists"}`
- **Error (404)**: `{"error": "Container 'Main-Container' not found"}`
- **Error (403)**: `{"error": "Not a Main Container"}`
- **JADE backend call**: AMS `CreateAgent` action via `JADEManagementOntology`

### 3.3 Kill Agent
- **Endpoint**: `DELETE /api/agents/{name}`
- **Input**: path param `name` (full AID, e.g. `rma`), optional query param `confirm=true`
- **Output (200)**: `{"message": "Agent 'rma@jade-main' killed"}`
- **Error (404)**: `{"error": "Agent not found"}`
- **Error (403)**: `{"error": "Cannot kill system agent"}` — cannot kill AMS/DFA
- **JADE backend call**: AMS `KillAgent` action via `JADEManagementOntology`

### 3.4 Suspend Agent
- **Endpoint**: `POST /api/agents/{name}/suspend`
- **Input**: path param `name`, empty body or `{"confirm": true}`
- **Output (200)**: `{"message": "Agent 'my-agent@jade-main' suspended"}`
- **Error (404)**: `{"error": "Agent not found"}`
- **JADE backend call**: AMS `Modify` with `AMSAgentDescription.state = SUSPENDED` via `FIPAManagementOntology`

### 3.5 Resume Agent
- **Endpoint**: `POST /api/agents/{name}/resume`
- **Input**: path param `name`
- **Output (200)**: `{"message": "Agent 'my-agent@jade-main' resumed"}`
- **Error (404)**: `{"error": "Agent not found"}`
- **JADE backend call**: AMS `Modify` with `AMSAgentDescription.state = ACTIVE` via `FIPAManagementOntology`

### 3.6 Freeze Agent
- **Endpoint**: `POST /api/agents/{name}/freeze`
- **Input**:
```json
{
  "container": "Buffer-Container",
  "repository": "file://./agent-store"
}
```
- **Output (200)**: `{"message": "Agent 'my-agent' frozen to 'Buffer-Container'"}`
- **Error (404)**: `{"error": "Agent or container not found"}`
- **Error (400)**: `{"error": "Container 'Buffer-Container' must be a buffer container"}`
- **JADE backend call**: `FreezeAgent` action via `PersistenceOntology`

### 3.7 Thaw Agent
- **Endpoint**: `POST /api/agents/{name}/thaw`
- **Input**:
```json
{
  "container": "Main-Container",
  "repository": "file://./agent-store"
}
```
- **Output (200)**: `{"message": "Agent 'my-agent' thawed to 'Main-Container'"}`
- **Error (404)**: `{"error": "Agent or container not found"}`
- **Error (400)**: `{"error": "Agent is not frozen"}`
- **JADE backend call**: `ThawAgent` action via `PersistenceOntology`

### 3.8 Clone Agent
- **Endpoint**: `POST /api/agents/clone`
- **Input**:
```json
{
  "name": "original-agent",
  "newName": "cloned-agent",
  "container": "Node1-Container"
}
```
- **Output (201)**: `{"message": "Agent 'cloned-agent@jade-main' cloned from 'original-agent'", "aid": "cloned-agent@jade-main"}`
- **Error (404)**: `{"error": "Agent or container not found"}`
- **JADE backend call**: `CloneAction` via `MobilityOntology`

### 3.9 Move Agent
- **Endpoint**: `POST /api/agents/{name}/move`
- **Input**:
```json
{
  "container": "Node1-Container"
}
```
- **Output (200)**: `{"message": "Agent 'my-agent' moved to 'Node1-Container'"}`
- **Error (404)**: `{"error": "Agent or container not found"}`
- **JADE backend call**: `MoveAction` via `MobilityOntology`

### 3.10 Save Agent
- **Endpoint**: `POST /api/agents/{name}/save`
- **Input**:
```json
{
  "repository": "file://./agent-store"
}
```
- **Output (200)**: `{"message": "Agent 'my-agent' saved to 'file://./agent-store'"}`
- **JADE backend call**: `SaveAgent` action via `PersistenceOntology`

### 3.11 Load Agent
- **Endpoint**: `POST /api/agents/load`
- **Input**:
```json
{
  "name": "my-agent",
  "container": "Main-Container",
  "repository": "file://./agent-store"
}
```
- **Output (200)**: `{"message": "Agent 'my-agent' loaded from 'file://./agent-store'"}`
- **JADE backend call**: `LoadAgent` action via `PersistenceOntology`

### 3.12 Change Agent Ownership
- **Endpoint**: `PATCH /api/agents/{name}`
- **Input**:
```json
{
  "ownership": "new-owner"
}
```
- **Output (200)**: `{"message": "Agent 'my-agent' ownership changed to 'new-owner'"}`
- **JADE backend call**: AMS `Modify` with `AMSAgentDescription.ownership` via `FIPAManagementOntology`

---

## 4. Remote Platform Operations

### 4.1 List Remote Platforms
- **Endpoint**: `GET /api/platforms`
- **Input**: none
- **Output (200)**:
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
- **JADE backend call**: `myPlatformProfile` from PlatformDescription

### 4.2 Add Remote Platform via AMS AID
- **Endpoint**: `POST /api/platforms`
- **Input**:
```json
{
  "ams": "ams@remote-platform",
  "addresses": ["jades://192.168.1.10:1099"]
}
```
- **Output (201)**: `{"message": "Platform 'remote-platform' added", "platform": {...}}`
- **Error (400)**: `{"error": "Invalid AMS or platform unreachable"}`
- **JADE backend call**: Send REQUEST to remote AMS to get AP description (GetDescription action via FIPAManagementOntology)

### 4.3 Add Remote Platform via URL
- **Endpoint**: `POST /api/platforms/fetch`
- **Input**:
```json
{
  "url": "http://192.168.1.10:8080/api/platform"
}
```
- **Output (201)**: `{"message": "Platform fetched from URL", "platform": {...}}`
- **Error (400)**: `{"error": "Failed to fetch platform description from URL"}`
- **JADE backend call**: HTTP GET on URL, parse APDescription

### 4.4 View AP Description
- **Endpoint**: `GET /api/platforms/{name}/description`
- **Input**: path param `name`
- **Output (200)**:
```json
{
  "name": "jade-main",
  "services": [
    {
      "type": "FIPAAgentManagement",
      "name": "fipa-agent-management",
      "addresses": ["jades://127.0.0.1:1099"]
    }
  ],
  "apServices": [...]
}
```
- **JADE backend call**: Return cached `APDescription` from `myPlatformProfile`

### 4.5 Refresh AP Description
- **Endpoint**: `POST /api/platforms/{name}/refresh`
- **Input**: none
- **Output (200)**: `{"message": "Platform description refreshed", "platform": {...}}`
- **JADE backend call**: Request fresh APDescription from remote AMS

### 4.6 Remove Remote Platform
- **Endpoint**: `DELETE /api/platforms/{name}`
- **Input**: path param `name`
- **Output (200)**: `{"message": "Platform 'remote-platform' removed"}`
- **JADE backend call**: Remove from local tracking (GUI state change)

### 4.7 Refresh Agent List (Remote Platform)
- **Endpoint**: `GET /api/platforms/{name}/agents`
- **Input**: path param `name`
- **Output (200)**:
```json
{
  "agents": [
    {
      "name": "my-agent@remote-platform",
      "addresses": ["jades://192.168.1.10:1100/jade-tools"]
    }
  ]
}
```
- **JADE backend call**: AMS Search on remote AMS for AMSAgentDescription

---

## 5. Tool Launch Operations

These endpoints start the respective GUI tool agents. The actual GUI rendering is delegated to the Swing tool; the React UI shows status/progress.

| # | Functionality | Endpoint | Input | Output |
|---|--------------|----------|-------|--------|
| 1.19 | Start Sniffer | `POST /api/tools/sniffer/start` | `{"container": "Main-Container", "preload":[...]}` | `{"message": "Sniffer started", "agent": "sniffer@jade-main"}` |
| 1.20 | Start DummyAgent | `POST /api/tools/dummy/start` | `{"container": "Main-Container"}` | `{"message": "DummyAgent started", "agent": "dummy@jade-main"}` |
| 1.21 | Start LoggerAgent | `POST /api/tools/logger/start` | `{"container": "Main-Container"}` | `{"message": "LoggerAgent started", "agent": "logger@jade-main"}` |
| 1.22 | Start Introspector | `POST /api/tools/introspector/start` | `{"agent": "my-agent@jade-main", "container": "Main-Container"}` | `{"message": "Introspector started", "agent": "introspector@jade-main"}` |
| 1.23 | Show DFGui | `POST /api/tools/df-gui/start` | `{"container": "Main-Container"}` | `{"message": "DF GUI started", "agent": "df@jade-main"}` |

**Errors for all tool launch**:
- `(404)` `{"error": "Container not found"}`
- `(400)` `{"error": "Tool already running"}`
- `(403)` `{"error": "Not a Main Container"}`

**JADE backend call**: All use `AMS CreateAgent` with the specific tool class name, launched on the specified container.

---

## 6. Custom Actions

### 6.1 Register Remote Agent with AMS
- **Endpoint**: `POST /api/agents/register-remote`
- **Input**:
```json
{
  "aid": "foreign-agent@foreign-platform",
  "addresses": ["jades://192.168.1.10:1200"]
}
```
- **Output (200)**: `{"message": "Agent 'foreign-agent@foreign-platform' registered with local AMS"}`
- **Error (400)**: `{"error": "Invalid or duplicate agent"}`
- **JADE backend call**: AMS `Register` action via `FIPAManagementOntology`

### 6.2 Change Agent Ownership (Custom)
- **Endpoint**: `PATCH /api/agents/{name}/ownership`
- **Input**:
```json
{
  "newOwner": "admin-user"
}
```
- **Output (200)**: `{"message": "Ownership of 'my-agent' changed to 'admin-user'"}`
- **JADE backend call**: AMS `Modify` with `AMSAgentDescription.ownership`

---

## Summary of REST Endpoints (RMA)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/version` | JADE version info |
| GET | `/api/platform` | Platform metadata |
| POST | `/api/platform/shutdown` | Shutdown platform |
| GET | `/api/containers` | List all containers |
| GET | `/api/containers/{name}` | Get single container by name |
| DELETE | `/api/containers/{name}` | Kill a container |
| POST | `/api/containers/{name}/save` | Save container state |
| POST | `/api/containers/{name}/load` | Load container state |
| GET | `/api/containers/{name}/mtps` | List MTPs on a container |
| POST | `/api/containers/{name}/mtps` | Install a new MTP |
| DELETE | `/api/containers/{name}/mtps/{address}` | Uninstall an MTP |
| GET | `/api/agents` | List agents (filter by container/state) |
| GET | `/api/agents/{name}` | Get single agent by name |
| POST | `/api/agents` | Start new agent |
| DELETE | `/api/agents/{name}` | Kill agent |
| POST | `/api/agents/{name}/suspend` | Suspend agent |
| POST | `/api/agents/{name}/resume` | Resume agent |
| POST | `/api/agents/{name}/freeze` | Freeze agent |
| POST | `/api/agents/{name}/thaw` | Thaw agent |
| POST | `/api/agents/clone` | Clone agent |
| POST | `/api/agents/{name}/move` | Move agent to container |
| POST | `/api/agents/{name}/save` | Save agent state |
| POST | `/api/agents/load` | Load agent |
| PATCH | `/api/agents/{name}` | Modify agent (ownership) |
| POST | `/api/agents/{name}/ownership` | Change agent ownership |
| GET | `/api/platforms` | List remote platforms |
| POST | `/api/platforms` | Add remote platform via AMS AID |
| POST | `/api/platforms/fetch` | Add remote platform via URL |
| GET | `/api/platforms/{name}/description` | Get AP description |
| POST | `/api/platforms/{name}/refresh` | Refresh AP description |
| DELETE | `/api/platforms/{name}` | Remove remote platform |
| GET | `/api/platforms/{name}/agents` | Get remote platform agents |
| POST | `/api/agents/register-remote` | Register remote agent with local AMS |
| POST | `/api/tools/sniffer/start` | Start Sniffer tool |
| POST | `/api/tools/dummy/start` | Start Dummy Agent |
| POST | `/api/tools/logger/start` | Start Logger Agent |
| POST | `/api/tools/introspector/start` | Start Introspector |
| POST | `/api/tools/df-gui/start` | Start DF GUI |
| POST | `/api/platform/shutdown` | Shutdown platform |
