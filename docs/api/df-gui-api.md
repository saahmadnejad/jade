# DF GUI REST API Specification

Detailed input/output specifications for DF GUI (Directory Facilitator GUI) functionalities.

The DF GUI manages the Directory Facilitator (yellow pages) service. The REST API allows registering, deregistering, modifying, viewing, searching agents, and managing DF federation.

---

## 1. DF Registration Management

### 1.1 List Registered Agents
- **Endpoint**: `GET /api/df/registrations`
- **Input**: optional query param `df` (DF AID, defaults to local default DF)
- **Output (200)**:
```json
{
  "df": "df@jade-main",
  "registrations": [
    {
      "name": "my-agent@jade-main",
      "addresses": ["jades://127.0.0.1:1099/jade-tools"],
      "resolvers": [],
      "services": [
        {
          "type": "weather-forecast",
          "name": "weather-service",
          "addresses": ["http://weather.example.com/api"]
        }
      ],
      "ownership": "init"
    }
  ]
}
```
- **JADE backend call**: DF `search()` with empty DFAgentDescription

### 1.2 Register Agent with DF
- **Endpoint**: `POST /api/df/registrations`
- **Input**:
```json
{
  "df": "df@jade-main",
  "agentName": "my-agent",
  "addresses": ["jades://127.0.0.1:1099/jade-tools"],
  "resolvers": [],
  "services": [
    {
      "type": "weather-forecast",
      "name": "weather-service",
      "addresses": ["http://weather.example.com/api"]
    }
  ]
}
```
- **Output (201)**:
```json
{
  "message": "Agent 'my-agent@jade-main' registered with DF 'df@jade-main'",
  "registration": {...same as input with resolved AID}
}
```
- **Error (400)**: `{"error": "Mandatory fields missing: name, services"}`
- **Error (409)**: `{"error": "Agent already registered with this DF"}`
- **JADE backend call**: DF `register()` via DFGUIAdapter / AMS Register action

### 1.3 Deregister Agent from DF
- **Endpoint**: `DELETE /api/df/registrations/{agentName}`
- **Input**: path param `agentName` (AID local part or full AID), optional query param `df`
- **Output (200)**: `{"message": "Agent 'my-agent@jade-main' deregistered from DF"}`
- **Error (404)**: `{"error": "Agent not registered with this DF"}`
- **JADE backend call**: DF `deregister()` via DFGUIAdapter

### 1.4 View Agent Description (in DF)
- **Endpoint**: `GET /api/df/registrations/{agentName}`
- **Input**: path param `agentName`, optional query param `df`
- **Output (200)**: full `DFAgentDescription` object (same schema as registration above)
- **Error (404)**: `{"error": "Agent not registered with this DF"}`
- **JADE backend call**: DF `search()` for specific agent

### 1.5 Modify Agent Description
- **Endpoint**: `PUT /api/df/registrations/{agentName}`
- **Input**:
```json
{
  "df": "df@jade-main",
  "addresses": ["jades://127.0.0.1:1099/jade-tools"],
  "services": [
    {
      "type": "updated-service-type",
      "name": "updated-service-name",
      "addresses": ["http://updated.example.com"]
    }
  ]
}
```
- **Output (200)**: `{"message": "Registration modified", "registration": {...}}`
- **Error (404)**: `{"error": "Agent not registered with this DF"}`
- **JADE backend call**: DF `modify()` via DFGUIAdapter

---

## 2. DF Search

### 2.1 Search Agents in DF
- **Endpoint**: `POST /api/df/search`
- **Input**:
```json
{
  "df": "df@jade-main",
  "constraints": {
    "maxDepth": 0,
    "maxResults": -1
  },
  "description": {
    "name": null,
    "services": [
      {
        "type": "weather-forecast"
      }
    ],
    "addresses": [],
    "resolvers": []
  }
}
```
- **Output (200)**:
```json
{
  "df": "df@jade-main",
  "results": [
    {
      "name": "my-agent@jade-main",
      "addresses": ["jades://127.0.0.1:1099/jade-tools"],
      "services": [
        {"type": "weather-forecast", "name": "weather-service", "addresses": ["http://..."]}
      ],
      "ownership": "init"
    }
  ]
}
```
- **Error (500)**: `{"error": "Search failed"}`
- **JADE backend call**: DF `search()` with constraints via DFGUIAdapter

### 2.2 Search Result Details
- **Endpoint**: `GET /api/df/search-results/{agentName}`
- **Input**: path param `agentName`, optional query param `df`
- **Output (200)**: full `DFAgentDescription` of the search result
- **Error (404)**: `{"error": "Agent not in search results"}`
- **JADE backend call**: Look up in cached `lastSearchResults`

---

## 3. DF Federation

### 3.1 List Parent DFs
- **Endpoint**: `GET /api/df/federation/parents`
- **Input**: optional query param `df`
- **Output (200)**:
```json
{
  "parents": [
    {
      "name": "parent-df@parent-platform",
      "addresses": ["jades://10.0.0.1:1099"]
    }
  ]
}
```
- **JADE backend call**: DF `getParents()` or similar federation query

### 3.2 List Child DFs
- **Endpoint**: `GET /api/df/federation/children`
- **Input**: optional query param `df`
- **Output (200)**:
```json
{
  "children": [
    {
      "name": "child-df@child-platform",
      "addresses": ["jades://10.0.0.2:1099"]
    }
  ]
}
```

### 3.3 Federate DF with Another DF
- **Endpoint**: `POST /api/df/federation`
- **Input**:
```json
{
  "df": "df@jade-main",
  "parentDF": "parent-df@parent-platform",
  "parentDFAddresses": ["jades://10.0.0.1:1099"],
  "thisDFDescription": {
    "name": "df@jade-main",
    "services": [...],
    "addresses": ["jades://127.0.0.1:1099"]
  }
}
```
- **Output (200)**: `{"message": "DF federated with parent-df@parent-platform"}`
- **Error (400)**: `{"error": "Invalid parent DF"}`
- **Error (500)**: `{"error": "Federation failed"}`
- **JADE backend call**: DF `federate()` via DFGUIAdapter

### 3.4 Deregister from Parent DF
- **Endpoint**: `DELETE /api/df/federation/{parentDFName}`
- **Input**: path param `parentDFName`, optional query param `df`
- **Output (200)**: `{"message": "Deregistered from parent DF 'parent-df@parent-platform'"}`
- **JADE backend call**: Send Deregister to parent DF

### 3.5 Deregister Child DF from This DF
- **Endpoint**: `DELETE /api/df/federation/children/{childDFName}`
- **Input**: path param `childDFName`
- **Output (200)**: `{"message": "Child DF 'child-df@child-platform' deregistered"}`
- **JADE backend call**: Remove child from federation

---

## 4. DF Description View

### 4.1 Get This DF's Description
- **Endpoint**: `GET /api/df/description`
- **Input**: optional query param `df` (default: local default DF)
- **Output (200)**:
```json
{
  "name": "df@jade-main",
  "addresses": ["jades://127.0.0.1:1099"],
  "services": [
    {
      "type": "FIPA-Service",
      "name": "fipa-df",
      "addresses": ["jades://127.0.0.1:1099/jade-df"]
    }
  ]
}
```

### 4.2 View AP Description (from RMA "View AP Description")
- **Endpoint**: `GET /api/df/ap-description`
- **Input**: optional query param `df`
- **Output (200)**: APDescription object
```json
{
  "name": "jade-main",
  "services": [
    {"type": "FIPAAgentManagement", "name": "...", "addresses": [...]}
  ]
}
```

---

## 5. DF Lifecycle

### 5.1 Exit DF (Kill DF Agent)
- **Endpoint**: `DELETE /api/tools/df-gui`
- **Input**: none
- **Output (200)**: `{"message": "DF agent killed"}`
- **JADE backend call**: AMS `KillAgent` on DF

### 5.2 Close GUI (Hide GUI, Keep DF Running)
- **Endpoint**: `POST /api/tools/df-gui/close`
- **Input**: none
- **Output (200)**: `{"message": "DF GUI closed, DF agent still running"}`
- **JADE backend call**: No backend action — GUI-only action

### 5.3 Get DF Status
- **Endpoint**: `GET /api/tools/df-gui/status`
- **Input**: none
- **Output (200)**:
```json
{
  "running": true,
  "agent": "df@jade-main",
  "container": "Main-Container",
  "registeredAgentCount": 15,
  "parentCount": 2,
  "childCount": 3
}
```
- **Output (200, not running)**: `{"running": false}`

---

## 6. DF Status / Polling

### 6.1 Refresh DF Registration Data
- **Endpoint**: `POST /api/df/refresh`
- **Input**: optional `{"df": "df@jade-main"}`
- **Output (200)**:
```json
{
  "message": "DF refreshed",
  "registrations": [...],
  "parents": [...],
  "children": [...]
}
```
- **JADE backend call**: Re-query DF for registrations, parents, children

---

## Summary of REST Endpoints (DF GUI)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/df/registrations` | List agents registered with DF |
| POST | `/api/df/registrations` | Register an agent with DF |
| DELETE | `/api/df/registrations/{agentName}` | Deregister an agent from DF |
| GET | `/api/df/registrations/{agentName}` | View a specific registration |
| PUT | `/api/df/registrations/{agentName}` | Modify an existing registration |
| POST | `/api/df/search` | Search agents in DF with constraints |
| GET | `/api/df/search-results/{agentName}` | Get search result details |
| GET | `/api/df/federation/parents` | List parent DFs |
| GET | `/api/df/federation/children` | List child DFs |
| POST | `/api/df/federation` | Federate with a parent DF |
| DELETE | `/api/df/federation/{parentDFName}` | Deregister from parent DF |
| DELETE | `/api/df/federation/children/{childDFName}` | Deregister child DF |
| GET | `/api/df/description` | Get this DF's description |
| GET | `/api/df/ap-description` | Get AP description for DF |
| POST | `/api/df/refresh` | Refresh all DF data |
| DELETE | `/api/tools/df-gui` | Kill the DF agent |
| POST | `/api/tools/df-gui/close` | Close DF GUI (keep agent running) |
| GET | `/api/tools/df-gui/status` | Get DF status |
