# Shared GUI Components REST API Specification

**Purpose:** Specifications for shared Swing GUI components used across all JADE tools (RMA, Sniffer, Log Manager, Introspector, Test Agent), and their API needs for the new React GUI. This is the work queue for implementing backend services that are shared/common across multiple tools. Update as endpoints are implemented.

---

## 1. AgentTree (Platform Structure Tree)

Package: `io.donbee.jade.gui`  
Used by: RMA, Sniffer, Log Manager, Introspector

### 1.1 Get Full Platform Tree
- **Endpoint**: `GET /api/platform/tree`
- **Input**: none
- **Output (200)**:
```json
{
  "platformID": "jade-main",
  "localPlatform": {
    "name": "jade-main",
    "containers": [
      {
        "name": "Main-Container",
        "address": "127.0.0.1:1099",
        "isMain": true,
        "agents": [
          {
            "name": "rma@jade-main",
            "class": "io.donbee.jade.tools.rma.rma",
            "state": "ACTIVE",
            "ownership": "init",
            "addresses": ["jades://127.0.0.1:1099/jade-tools"],
            "isFrozen": false,
            "isSuspended": false
          },
          {
            "name": "ams@jade-main",
            "class": "io.donbee.jade.domain.ams",
            "state": "ACTIVE",
            "ownership": "init",
            "addresses": [],
            "isFrozen": false,
            "isSuspended": false
          }
        ],
        "mtps": ["jade.mtp.tcl.TcpMTP"]
      }
    ]
  },
  "remotePlatforms": [
    {
      "ams": "ams@remote-platform",
      "name": "remote-platform",
      "addresses": [],
      "services": [],
      "agents": []
    }
  ]
}
```
- **JADE backend call**: AMS subscription + `getPlatformID()` + `getID()` + `getAMS()`

### 1.2 WebSocket: Real-time Tree Updates
- **Endpoint**: `WS /api/platform/tree/ws`
- **Output (events)**:
```json
{
  "type": "AGENT_BORN",
  "container": "Main-Container",
  "agent": {
    "name": "new-agent@jade-main",
    "class": "com.example.NewAgent",
    "state": "ACTIVE"
  }
}
```
```json
{
  "type": "AGENT_DEAD",
  "agent": "old-agent@jade-main"
}
```
```json
{
  "type": "AGENT_MOVED",
  "agent": "moved-agent@jade-main",
  "fromContainer": "Node1-Container",
  "toContainer": "Main-Container"
}
```
```json
{
  "type": "CONTAINER_ADDED",
  "container": {"name": "Node1-Container", "address": "127.0.0.1:1098"}
}
```
```json
{
  "type": "CONTAINER_REMOVED",
  "container": "Node1-Container"
}
```
```json
{
  "type": "AGENT_SUSPENDED",
  "agent": "suspended-agent@jade-main"
}
```
```json
{
  "type": "AGENT_RESUMED",
  "agent": "resumed-agent@jade-main"
}
```
```json
{
  "type": "AGENT_FROZEN",
  "agent": "frozen-agent@jade-main",
  "fromContainer": "Main-Container",
  "toContainer": "Buffer-Container"
}
```
```json
{
  "type": "AGENT_THAWED",
  "agent": "thawed-agent@jade-main",
  "fromContainer": "Buffer-Container",
  "toContainer": "Main-Container"
}
```
```json
{
  "type": "MTP_ADDED",
  "container": "Main-Container",
  "address": "jades://127.0.0.1:1100"
}
```
```json
{
  "type": "MTP_REMOVED",
  "container": "Main-Container",
  "address": "jades://127.0.0.1:1100"
}
```
- **JADE backend call**: AMS introspection events subscription

---

## 2. AclGui (ACL Message Composition Component)

Package: `io.donbee.jade.gui`  
Used by: Dummy Agent, Test Agent, RMA error dialog

### 2.1 Parse ACL Message from String
- **Endpoint**: `POST /api/acl/parse`
- **Input**:
```json
{
  "content": "(inform :sender (agent-identifier :name ams) :receiver (agent-identifier :name agent1) :content \"((action ... ))\")",
  "encoding": "Base64"
}
```
- **Output (200)**:
```json
{
  "message": {
    "id": 1,
    "performative": 3,
    "performativeName": "INFORM",
    "sender": {"name": "ams@jade-main"},
    "receivers": [{"name": "agent1@jade-main"}],
    "replyTo": [],
    "content": "(inform ...)",
    "language": null,
    "ontology": null,
    "protocol": null,
    "conversationId": null,
    "replyWith": null,
    "inReplyTo": null,
    "encoding": null,
    "envelope": null
  }
}
```
- **Error (400)**: `{"error": "Failed to parse ACL message: ..."}`
- **JADE backend call**: `StringACLCodec.decode()`

### 2.2 Serialize ACL Message to String
- **Endpoint**: `POST /api/acl/serialize`
- **Input**:
```json
{
  "message": {
    "performative": "INFORM",
    "sender": "agent1@jade-main",
    "receivers": ["agent2@jade-main"],
    "content": "hello"
  }
}
```
- **Output (200)**:
```json
{
  "serialized": "(inform :sender (agent-identifier :name agent1@jade-main) :receiver (agent-identifier :name agent2@jade-main) :content \"hello\")"
}
```
- **JADE backend call**: `StringACLCodec.encode()`

### 2.3 Get AID from Input
- **Endpoint**: `GET /api/acl/aid-suggestions?q={prefix}`
- **Input**: query param `q` (search prefix)
- **Output (200)**:
```json
{
  "suggestions": [
    {"name": "agent1@jade-main", "addresses": ["jades://127.0.0.1:1099/jade-tools"]},
    {"name": "agent2@jade-main", "addresses": ["jades://127.0.0.1:1099/jade-tools"]}
  ]
}
```
- **JADE backend call**: AMS `search()` for AID matching prefix

### 2.4 View ACL Message in Dialog
- **Endpoint**: `GET /api/acl/message/{id}/formatted`
- **Input**: path param `id`
- **Output (200)**:
```json
{
  "performative": "INFORM",
  "sender": "agent1@jade-main",
  "receiver": "agent2@jade-main",
  "content": "(inform ...)",
  "language": "FIPA/SL",
  "ontology": "FIPA-Agent-Management",
  "fullString": "(inform :sender ... :content ...)"
}
```

---

## 3. AIDGui (Agent Identifier Editor)

Package: `io.donbee.jade.gui`

### 3.1 Parse AID from String
- **Endpoint**: `POST /api/aid/parse`
- **Input**:
```json
{
  "name": "agent1@platform1"
}
```
- **Output (200)**:
```json
{
  "aid": {
    "name": "agent1@platform1",
    "addresses": ["jades://host:port"]
  }
}
```

### 3.2 Resolve AID by Name
- **Endpoint**: `GET /api/aid/{name}`
- **Input**: path param `name`
- **Output (200)**:
```json
{
  "name": "agent1@jade-main",
  "addresses": ["jades://127.0.0.1:1099/jade-tools"],
  "isLocal": true,
  "container": "Main-Container"
}
```
- **Error (404)**: `{"error": "Agent not found"}`

---

## 4. APDescriptionPanel (Platform Description Viewer)

Package: `io.donbee.jade.gui`  
Used by: RMA (View AP Description), DF GUI

### 4.1 Get AP Description
- **Endpoint**: `GET /api/platform/description{descriptionType=local|remote}`
- **Input**: optional query param `descriptionType`
  - `local` (default) — local platform description
  - `remote` — requires `platform` query param
  - optional `platform` query param for remote platform name
- **Output (200)**:
```json
{
  "name": "jade-main",
  "services": [
    {
      "type": "FIPAAgentManagement",
      "name": "fipa-agent-management",
      "addresses": ["jades://127.0.0.1:1099/jade"]
    },
    {
      "type": "Mobility",
      "name": "agent-mobility",
      "addresses": ["jades://127.0.0.1:1099/jade"]
    },
    {
      "type": "Messaging",
      "name": "message-manager",
      "addresses": ["jades://127.0.0.1:1099/jade"]
    }
  ],
  "addresses": ["127.0.0.1:1099"]
}
```
- **JADE backend call**: `APDescription` from PlatformDescription event

---

## 5. Dialog Components

### 5.1 String Dialog — Generic String Input
- **Endpoint**: `GET /api/dialog/string`
- **Input**: query params:
  - `title` — dialog title
  - `label` — input label text
  - `defaultValue` — default string value
  - `message` — prompt message
- **Output**: This is a client-side dialog — no REST endpoint needed.  
  **Implementation note**: React modal dialog component.

### 5.2 Time Chooser
- **Component**: Time selection dialog for setting agent wake-up times
- **Implementation note**: React datetime picker. No REST needed.

### 5.3 Search Constraints Dialog
- **Endpoint**: `GET /api/df/constraints`
- **Input**: none (returns default constraint values)
- **Output (200)**:
```json
{
  "maxDepth": 0,
  "maxResults": -1,
  "relative": false
}
```
- **JADE backend call**: `SearchConstraints` defaults

---

## 6. About JADE Action

### 6.1 Get About JADE Info
- **Endpoint**: `GET /api/about`
- **Input**: none
- **Output (200)**:
```json
{
  "version": "14.0",
  "revision": "6852",
  "date": "2024-01-15T10:00:00Z",
  "copyright": "JADE - Java Agent DEvelopment Framework",
  "license": "LGPL 2.1",
  "url": "https://jade.tilab.com/"
}
```
- **JADE backend call**: `VersionManager.getVersion()`, etc.

---

## 7. Service Description Dialog

### 7.1 Get Service Types
- **Endpoint**: `GET /api/services/types`
- **Input**: none
- **Output (200)**:
```json
{
  "types": ["FIPAAgentManagement", "Mobility", "Messaging", "Persistence", "Logging"]
}
```

### 7.2 Add Service Description
- **Endpoint**: `POST /api/df/service`
- **Input**:
```json
{
  "type": "weather-forecast",
  "name": "weather-service",
  "addresses": ["http://weather.example.com/api"]
}
```
- **Output (200)**: `{"message": "Service description created"}`
- **Note**: This is typically used as part of DF registration — the service description is embedded in the registration request.

---

## 8. Property List Editors

### 8.1 Get Editable Properties
- **Endpoint**: `GET /api/properties/schema`
- **Input**: query param `type` — `agent`, `container`, `df`, `mtp`
- **Output (200)**:
```json
{
  "type": "agent",
  "properties": [
    {"name": "name", "type": "STRING", "required": true, "editable": true},
    {"name": "class", "type": "STRING", "required": true, "editable": true},
    {"name": "ownership", "type": "STRING", "required": false, "editable": true},
    {"name": "container", "type": "STRING", "required": true, "editable": true}
  ]
}
```
- **Implementation note**: This is primarily a form schema for the React UI.

---

## Summary of REST Endpoints (Shared Components)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/platform/tree` | Get full platform tree (containers, agents) |
| WS | `/api/platform/tree/ws` | WebSocket: real-time tree events |
| POST | `/api/acl/parse` | Parse ACL string to message object |
| POST | `/api/acl/serialize` | Serialize message object to ACL string |
| GET | `/api/acl/aid-suggestions?q={prefix}` | AID autocomplete suggestions |
| GET | `/api/acl/message/{id}/formatted` | Get formatted ACL message |
| POST | `/api/aid/parse` | Parse AID from string |
| GET | `/api/aid/{name}` | Resolve AID by name |
| GET | `/api/platform/description` | Get AP description (local or remote) |
| GET | `/api/about` | Get JADE about/version info |
| GET | `/api/df/constraints` | Get default search constraints |
| GET | `/api/services/types` | Get available service types |
| POST | `/api/df/service` | Add a service description (embedded in registration) |
| GET | `/api/properties/schema?type={type}` | Get property schema for form building |
