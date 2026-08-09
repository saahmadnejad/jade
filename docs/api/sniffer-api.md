# Sniffer REST API Specification

Detailed input/output specifications for Sniffer functionalities.

The Sniffer intercepts ACL messages exchanged between agents. The React UI needs endpoints to configure sniffing, view caught messages, and manage the sniffer lifecycle.

---

## 1. Sniffing Control

### 1.1 Start Sniffing Agents
- **Endpoint**: `POST /api/sniffer/agents`
- **Input**:
```json
{
  "agents": ["agent1", "agent2"],
  "container": "Main-Container",
  "performativeFilters": ["INFORM", "REQUEST"]
}
```
- **Output (200)**:
```json
{
  "message": "Started sniffing 2 agents",
  "sniffedAgents": ["agent1@jade-main", "agent2@jade-main"],
  "snifferAgent": "sniffer@jade-main"
}
```
- **Error (400)**: `{"error": "One or more agents not found"}`
- **Error (403)**: `{"error": "Sniffer agent not running"}`
- **JADE backend call**: Send `SniffOn` action to AMS via `JADEManagementOntology`

### 1.2 Stop Sniffing Agents
- **Endpoint**: `DELETE /api/sniffer/agents`
- **Input**:
```json
{
  "agents": ["agent1", "agent2"]
}
```
- **Output (200)**:
```json
{
  "message": "Stopped sniffing 2 agents",
  "remainingAgents": ["agent3@jade-main"]
}
```
- **JADE backend call**: Send `SniffOff` action to AMS

### 1.3 Show Only (Filter)
- **Endpoint**: `PUT /api/sniffer/filter`
- **Input**:
```json
{
  "agents": ["agent1"],
  "mode": "SHOW_ONLY"
}
```
- **Output (200)**: `{"message": "Filter set to show only agent1"}`
- **JADE backend call**: Update sniffer filter list (local config)

### 1.4 Get Sniffed Agents
- **Endpoint**: `GET /api/sniffer/agents`
- **Input**: none
- **Output (200)**:
```json
{
  "sniffedAgents": ["agent1@jade-main", "agent2@jade-main"],
  "nonSniffedAgents": ["agent3@jade-main"]
}
```
- **JADE backend call**: Read sniffer agent's `agentsUnderSniff` list

### 1.5 Exit Sniffer
- **Endpoint**: `DELETE /api/tools/sniffer`
- **Input**: none
- **Output (200)**: `{"message": "Sniffer agent deleted"}`
- **JADE backend call**: Kill sniffer agent via AMS KillAgent

---

## 2. Captured Messages API

### 2.1 List All Captured Messages
- **Endpoint**: `GET /api/sniffer/messages`
- **Input**: optional query params:
  - `sender` — filter by sender AID
  - `receiver` — filter by receiver AID
  - `performative` — filter by performative
  - `direction` — `INCOMING` or `OUTGOING`
  - `limit` — max messages (default 1000)
  - `offset` — pagination offset
- **Output (200)**:
```json
{
  "messages": [
    {
      "id": "msg-001",
      "timestamp": "2024-01-15T10:30:00Z",
      "direction": "OUTGOING",
      "sender": "agent1@jade-main",
  
      "receiver": "agent2@jade-main",
      "performative": 3,
      "performativeName": "INFORM",
      "language": "FIPA/SL",
      "ontology": "FIPA-Agent-Management",
      "content": "(action ...)",
      "conversationId": "conv-123",
      "replyWith": "reply-456",
      "inReplyTo": "reply-123",
      "encoding": "UTF-8",
      "envelopes": [
        {
          "payloadEncoding": "Base64",
          "payload": "..."
        }
      ]
    }
  ],
  "total": 1500,
  "limit": 1000,
  "offset": 0
}
```
- **JADE backend call**: Read from sniffer's message canvas / ACL message storage

### 2.2 Get Single Message
- **Endpoint**: `GET /api/sniffer/messages/{id}`
- **Input**: path param `id`
- **Output (200)**: full ACLMessage object (same schema as above item in messages array)
- **Error (404)**: `{"error": "Message not found"}`

### 2.3 Clear Canvas (Clear All Messages)
- **Endpoint**: `DELETE /api/sniffer/messages`
- **Input**: optional query param `confirm=true`
- **Output (200)**: `{"message": "All messages cleared from canvas"}`
- **JADE backend call**: Clear sniffer's message canvas

---

## 3. Log File Management

### 3.1 Write Log File
- **Endpoint**: `POST /api/sniffer/log/write`
- **Input**:
```json
{
  "filename": "/path/to/sniffer-log.txt",
  "format": "TEXT"
}
```
- **Output (200)**:
```json
{
  "message": "Log written to /path/to/sniffer-log.txt",
  "messageCount": 1500
}
```
- **Error (500)**: `{"error": "Failed to write log file"}`
- **JADE backend call**: Write captured messages to file

### 3.2 Display Log File
- **Endpoint**: `POST /api/sniffer/log/load`
- **Input**:
```json
{
  "filename": "/path/to/sniffer-log.txt"
}
```
- **Output (200)**:
```json
{
  "message": "Log file loaded",
  "messageCount": 1500,
  "messages": [...]
}
```
- **Error (404)**: `{"error": "Log file not found"}`
- **JADE backend call**: Parse and load messages from file

### 3.3 Write Message List
- **Endpoint**: `POST /api/sniffer/messages/export`
- **Input**:
```json
{
  "filename": "/path/to/messages.txt",
  "filters": {
    "sender": "agent1",
    "performative": "INFORM"
  }
}
```
- **Output (200)**:
```json
{
  "message": "Message list exported",
  "count": 500
}
```
- **Error (500)**: `{"error": "Failed to export messages"}`

---

## 4. Agent Tree

### 4.1 Get Platform Agent Tree
- **Endpoint**: `GET /api/sniffer/tree`
- **Input**: none
- **Output (200)**:
```json
{
  "containers": [
    {
      "name": "Main-Container",
      "agents": [
        {"name": "agent1@jade-main", "isSniffed": true},
        {"name": "agent2@jade-main", "isSniffed": false}
      ]
    },
    {
      "name": "Node1-Container",
      "agents": [...]
    }
  ]
}
```
- **JADE backend call**: AMS subscription to introspection events

---

## 5. Preload Configuration

### 5.1 Get Preload Configuration
- **Endpoint**: `GET /api/sniffer/preload`
- **Input**: none
- **Output (200)**:
```json
{
  "preload": [
    {
      "agentPattern": "my-agent*",
      "performativeFilters": ["INFORM", "REQUEST"]
    }
  ],
  "clipPrefixes": ["com.example."]
}
```
- **JADE backend call**: Read sniffer properties / config file

### 5.2 Set Preload Configuration
- **Endpoint**: `PUT /api/sniffer/preload`
- **Input**:
```json
{
  "preload": [
    {
      "agentPattern": "test-agent*",
      "performativeFilters": ["INFORM"]
    }
  ]
}
```
- **Output (200)**: `{"message": "Preload configuration updated"}`
- **JADE backend call**: Write to sniffer properties (affects next sniffer start)

---

## 6. Sniffer Agent Management

### 6.1 Start Sniffer Agent
- **Endpoint**: `POST /api/tools/sniffer/start`
- **Input**:
```json
{
  "container": "Main-Container",
  "preload": "agent1;agent2 INFORM REQUEST",
  "clip": "com.example."
}
```
- **Output (201)**:
```json
{
  "message": "Sniffer started",
  "agent": "sniffer@jade-main",
  "gui": {
    "type": "Swing",
    "status": "launched"
  }
}
```
- **Error (400)**: `{"error": "Sniffer already running"}`
- **JADE backend call**: AMS `CreateAgent` for `io.donbee.jade.tools.sniffer.Sniffer`

### 6.2 Get Sniffer Status
- **Endpoint**: `GET /api/tools/sniffer/status`
- **Input**: none
- **Output (200)**:
```json
{
  "running": true,
  "agent": "sniffer@jade-main",
  "container": "Main-Container",
  "sniffedAgentCount": 5,
  "capturedMessageCount": 1500
}
```
- **Output (200, not running)**:
```json
{
  "running": false
}
```
- **JADE backend call**: Check if sniffer agent exists and get its state

---

## 7. Message Actions (Popup Context)

### 7.1 View Message Sender/Receiver Details
- **Endpoint**: `GET /api/sniffer/messages/{id}/participants`
- **Input**: path param `id`
- **Output (200)**:
```json
{
  "sender": {
    "name": "agent1@jade-main",
    "addresses": ["jades://127.0.0.1:1099/jade-tools"]
  },
  "receivers": [
    {
      "name": "agent2@jade-main",
      "addresses": ["jades://127.0.0.1:1099/jade-tools"]
    }
  ]
}
```

### 7.2 Save Message to File
- **Endpoint**: `POST /api/sniffer/messages/{id}/save`
- **Input**:
```json
{
  "filename": "/path/to/message.acl"
}
```
- **Output (200)**: `{"message": "Message saved to /path/to/message.acl"}`

---

## Summary of REST Endpoints (Sniffer)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tools/sniffer/start` | Start sniffer agent |
| GET | `/api/tools/sniffer/status` | Get sniffer status |
| DELETE | `/api/tools/sniffer` | Kill/exit sniffer |
| POST | `/api/sniffer/agents` | Start sniffing agents |
| DELETE | `/api/sniffer/agents` | Stop sniffing agents |
| GET | `/api/sniffer/agents` | Get sniffed/non-sniffed agents list |
| PUT | `/api/sniffer/filter` | Set "show only" filter |
| GET | `/api/sniffer/messages` | List captured messages |
| GET | `/api/sniffer/messages/{id}` | Get single message |
| DELETE | `/api/sniffer/messages` | Clear all captured messages |
| GET | `/api/sniffer/messages/{id}/participants` | Get message sender/receivers |
| POST | `/api/sniffer/messages/{id}/save` | Save single message to file |
| POST | `/api/sniffer/messages/export` | Export (filtered) messages to file |
| POST | `/api/sniffer/log/write` | Write all sniffed messages to log file |
| POST | `/api/sniffer/log/load` | Load messages from a log file |
| GET | `/api/sniffer/tree` | Get platform agent tree |
| GET | `/api/sniffer/preload` | Get preload configuration |
| PUT | `/api/sniffer/preload` | Set preload configuration |
