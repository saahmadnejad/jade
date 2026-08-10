# Introspector REST API Specification

**Purpose:** Specification for the Introspector (Agent Debugger) tool's REST endpoints. This is the work queue for implementing the debugger's backend API — message tracing, behaviour tree inspection, and execution control (step, break, slow, go). Update as endpoints are implemented.

Detailed input/output specifications for Introspector (Agent Debugger) functionalities.

The Introspector attaches to a target agent and provides debugging capabilities: message tracing, behaviour tree inspection, and execution control (step, break, slow, go).

---

## 1. Introspector Lifecycle

### 1.1 Start Introspector on an Agent
- **Endpoint**: `POST /api/tools/introspector/start`
- **Input**:
```json
{
  "agent": "my-agent@jade-main",
  "container": "Main-Container"
}
```
- **Output (201)**:
```json
{
  "message": "Introspector started for agent 'my-agent'",
  "agent": "introspector@jade-main",
  "debuggedAgent": "my-agent@jade-main"
}
```
- **Error (404)**: `{"error": "Agent 'my-agent@jade-main' not found"}`
- **Error (400)**: `{"error": "Introspector already running for this agent"}`
- **JADE backend call**: AMS `CreateAgent` for `Introspector` agent with argument = target agent AID

### 1.2 Get Introspector Status
- **Endpoint**: `GET /api/tools/introspector/status`
- **Input**: none
- **Output (200)**:
```json
{
  "running": true,
  "agent": "introspector@jade-main",
  "debuggedAgent": "my-agent@jade-main",
  "container": "Main-Container",
  "state": "RUNNING"
}
```
- **Output (200, not running)**: `{"running": false}`

### 1.3 Exit Introspector
- **Endpoint**: `DELETE /api/tools/introspector`
- **Input**: none
- **Output (200)**: `{"message": "Introspector agent deleted"}`
- **JADE backend call**: AMS `KillAgent` on introspector

---

## 2. Debug Control Commands

### 2.1 Step — Execute One Behaviour Step
- **Endpoint**: `POST /api/introspector/{agent}/step`
- **Input**: path param `agent`, empty body
- **Output (200)**:
```json
{
  "message": "Step command sent",
  "agent": "my-agent@jade-main",
  "behaviourExecuted": "com.example.MyBehaviour"
}
```
- **JADE backend call**: Send `STEP` event to introspector via ACL message

### 2.2 Break — Pause Agent Execution
- **Endpoint**: `POST /api/introspector/{agent}/break`
- **Input**: path param `agent`, empty body
- **Output (200)**: `{"message": "Break command sent to agent 'my-agent@jade-main'"}`
- **JADE backend call**: Send `BREAK` event to introspector

### 2.3 Slow — Execute with Delay
- **Endpoint**: `POST /api/introspector/{agent}/slow`
- **Input**:
```json
{
  "delayMs": 500
}
```
- **Output (200)**: `{"message": "Slow mode activated with delay=500ms"}`
- **JADE backend call**: Send `SLOW` event to introspector

### 2.4 Go — Resume Normal Execution
- **Endpoint**: `POST /api/introspector/{agent}/go`
- **Input**: path param `agent`, empty body
- **Output (200)**: `{"message": "Go command sent"}`
- **JADE backend call**: Send `GO` event to introspector

---

## 3. Message Display

### 3.1 List Messages (all 4 categories)
- **Endpoint**: `GET /api/introspector/{agent}/messages`
- **Input**: path param `agent`, optional query param `type` (`INCOMING_PENDING`, `INCOMING_RECEIVED`, `OUTGOING_PENDING`, `OUTGOING_SENT`, or `ALL`)
- **Output (200)**:
```json
{
  "agent": "my-agent@jade-main",
  "incomingPending": [...],
  "incomingReceived": [...],
  "outgoingPending": [...],
  "outgoingSent": [...]
}
```
Each message in the arrays follows the same schema:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "sender": "sender@jade-main",
  "receivers": ["receiver@jade-main"],
  "performative": 3,
  "performativeName": "INFORM",
  "content": "(action ...)",
  "language": "FIPA/SL",
  "ontology": "FIPA-Agent-Management",
  "conversationId": "conv-123"
}
```
- **JADE backend call**: Read from introspector's message tables

### 3.2 Get Single Message Detail
- **Endpoint**: `GET /api/introspector/{agent}/messages/{id}`
- **Input**: path params `agent`, `id`
- **Output (200)**: full message object with detailed envelope info
- **Error (404)**: `{"error": "Message not found"}`

---

## 4. Behaviour Tree

### 4.1 Get Full Behaviour Tree
- **Endpoint**: `GET /api/introspector/{agent}/behaviours`
- **Input**: path param `agent`
- **Output (200)**:
```json
{
  "agent": "my-agent@jade-main",
  "root": {
    "name": "RootBehaviour",
    "className": "com.example.MyAgent$RootBehaviour",
    "state": "ACTIVE",
    "children": [
      {
        "name": "CyclicBehaviour-1",
        "className": "io.donbee.jade.core.behaviours.CyclicBehaviour",
        "state": "RUNNING",
        "children": []
      },
      {
        "name": "OneShotBehaviour-1",
        "className": "io.donbee.jade.core.behaviours.OneShotBehaviour",
        "state": "DONE",
        "children": []
      }
    ]
  }
}
```
- **JADE backend call**: Query introspector for behaviour tree structure

### 4.2 Get Behaviour State Changes
- **Endpoint**: `GET /api/introspector/{agent}/behaviours/events`
- **Input**: path param `agent`, optional query param `since` (timestamp)
- **Output (200)**:
```json
{
  "events": [
    {
      "timestamp": "2024-01-15T10:30:01Z",
      "behaviour": "CyclicBehaviour-1",
      "oldState": "RUNNING",
      "newState": "BLOCKED"
    }
  ]
}
```
- **JADE backend call**: Poll introspector for recent behaviour events

---

## 5. Agent State

### 5.1 Get Agent State
- **Endpoint**: `GET /api/introspector/{agent}/state`
- **Input**: path param `agent`
- **Output (200)**:
```json
{
  "agent": "my-agent@jade-main",
  "state": "ACTIVE",
  "ownership": "init",
  "container": "Main-Container",
  "behaviourCount": 5,
  "messageQueueSize": 3,
  "isFrozen": false,
  "isSuspended": false
}
```
- **JADE backend call**: Query AMS for agent state + introspector for debug state

---

## 6. View Options

### 6.1 Toggle Message Display
- **Endpoint**: `PATCH /api/introspector/{agent}/view`
- **Input**:
```json
{
  "showMessages": true,
  "showBehaviours": true
}
```
- **Output (200)**: `{"message": "View settings updated", "showMessages": true, "showBehaviours": true}`
- **JADE backend call**: This is a GUI-only toggle; no backend ACL message needed

### 6.2 Get Current View Settings
- **Endpoint**: `GET /api/introspector/{agent}/view`
- **Input**: path param `agent`
- **Output (200)**:
```json
{
  "showMessages": true,
  "showBehaviours": true
}
```

---

## Summary of REST Endpoints (Introspector)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tools/introspector/start` | Start introspector on an agent |
| GET | `/api/tools/introspector/status` | Get introspector status |
| DELETE | `/api/tools/introspector` | Kill introspector agent |
| POST | `/api/introspector/{agent}/step` | Step — execute one behaviour |
| POST | `/api/introspector/{agent}/break` | Break — pause execution |
| POST | `/api/introspector/{agent}/slow` | Slow — step with delay |
| POST | `/api/introspector/{agent}/go` | Go — resume execution |
| GET | `/api/introspector/{agent}/messages` | List all messages (4 categories) |
| GET | `/api/introspector/{agent}/messages/{id}` | Get single message detail |
| GET | `/api/introspector/{agent}/behaviours` | Get full behaviour tree |
| GET | `/api/introspector/{agent}/behaviours/events` | Get behaviour state change events |
| GET | `/api/introspector/{agent}/state` | Get agent debug state |
| PATCH | `/api/introspector/{agent}/view` | Toggle message/behaviour display |
| GET | `/api/introspector/{agent}/view` | Get current view settings |
