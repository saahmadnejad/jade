# Test Agent REST API Specification

**Purpose:** Specification for the Test Agent tool's REST endpoints. This is the work queue for implementing the test agent's backend API — message composition, trace management, and automated test execution. Update as endpoints are implemented.

Detailed input/output specifications for Test Agent functionalities.

The Test Agent is a test/debug tool for sending and receiving ACL messages. It includes a message composition panel (ACLPanel), an ACL trace/history view (ACLTracePanel), and message file I/O capabilities.

---

## 1. Test Agent Lifecycle

### 1.1 Start Test Agent
- **Endpoint**: `POST /api/tools/testagent/start`
- **Input**:
```json
{
  "container": "Main-Container",
  "name": "test-agent1"
}
```
- **Output (201)**:
```json
{
  "message": "Test Agent started",
  "agent": "test-agent1@jade-main",
  "container": "Main-Container"
}
```
- **Error (400)**: `{"error": "Test Agent already running"}`
- **JADE backend call**: AMS `CreateAgent` for `io.donbee.jade.tools.testagent.TestAgent`

### 1.2 Get Test Agent Status
- **Endpoint**: `GET /api/tools/testagent/status`
- **Input**: none
- **Output (200)**:
```json
{
  "running": true,
  "agent": "test-agent1@jade-main",
  "container": "Main-Container",
  "receivedCount": 42,
  "sentCount": 17
}
```

### 1.3 Exit Test Agent
- **Endpoint**: `DELETE /api/tools/testagent`
- **Input**: none
- **Output (200)**: `{"message": "Test Agent deleted"}`

---

## 2. Message Composition (ACLPanel)

### 2.1 Get Current Compose Message
- **Endpoint**: `GET /api/testagent/{agent}/compose`
- **Input**: path param `agent`
- **Output (200)**:
```json
{
  "message": {
    "performative": 3,
    "performativeName": "INFORM",
    "sender": "test-agent1@jade-main",
    "receivers": ["receiver1@jade-main"],
    "replyTo": [],
    "content": "(inform ...)",
    "language": "FIPA/SL",
    "ontology": "FIPA-Agent-Management",
    "protocol": null,
    "conversationId": "conv-001",
    "replyWith": "",
    "inReplyTo": "",
    "encoding": "Base64"
  }
}
```

### 2.2 Set Compose Message
- **Endpoint**: `PUT /api/testagent/{agent}/compose`
- **Input**:
```json
{
  "performative": "REQUEST",
  "sender": "test-agent1@jade-main",
  "receivers": ["target-agent@jade-main"],
  "content": "(request :action (inform ...))",
  "language": "FIPA/SL",
  "ontology": "FIPA-Agent-Management",
  "protocol": "fipa-request",
  "conversationId": "conv-002",
  "replyWith": "req-001",
  "inReplyTo": "",
  "encoding": "Base64",
  "replyTo": [],
  "envelope": {}
}
```
- **Output (200)**: `{"message": "Compose message updated"}`

### 2.3 Create New Empty Message
- **Endpoint**: `POST /api/testagent/{agent}/compose/new`
- **Input**: optional:
```json
{
  "performative": "INFORM",
  "sender": "test-agent1@jade-main"
}
```
- **Output (200)**:
```json
{
  "message": "New message created",
  "message": {
    "performative": 3,
    "performativeName": "INFORM",
    "sender": "test-agent1@jade-main",
    "receivers": [],
    "content": ""
  }
}
```

### 2.4 Send Compose Message
- **Endpoint**: `POST /api/testagent/{agent}/compose/send`
- **Input**: none (sends current message)
- **Output (200)**:
```json
{
  "message": "Message sent",
  "id": 25,
  "performative": "INFORM",
  "sender": "test-agent1@jade-main",
  "receivers": ["receiver1@jade-main"]
}
```
- **Error (400)**: `{"error": "No receivers specified"}`
- **Error (404)**: `{"error": "One or more receivers not found"}`

### 2.5 Create Reply to a Received Message
- **Endpoint**: `POST /api/testagent/{agent}/compose/reply/{messageId}`
- **Input**: path params `agent`, `messageId`
- **Output (200)**:
```json
{
  "message": "Reply created",
  "composeMessage": {
    "performative": "INFORM",
    "sender": "test-agent1@jade-main",
    "receivers": ["original-sender@jade-main"],
    "inReplyTo": "original-in-reply-to",
    "conversationId": "original-conv-id"
  }
}
```
- **Error (404)**: `{"error": "Original message not found"}`

### 2.6 Get Available Performatives
- **Endpoint**: `GET /api/testagent/performatives`
- **Input**: none
- **Output (200)**:
```json
{
  "performatives": ["ACCEPT-PERFORMATIVE", "AGREE", "CANCEL", ... "INFORM", "REQUEST", ...]
}
```

---

## 3. ACL Trace (ACLTracePanel)

### 3.1 Get Message Trace
- **Endpoint**: `GET /api/testagent/{agent}/trace`
- **Input**: path param `agent`, optional query params:
  - `direction` — `INCOMING`, `OUTGOING`, `ALL` (default: `ALL`)
  - `limit` — max messages (default 500)
  - `offset` — pagination (default 0)
  - `performative` — filter by performative name
- **Output (200)**:
```json
{
  "trace": [
    {
      "id": "msg-1001",
      "timestamp": "2024-01-15T10:30:00Z",
      "direction": "INCOMING",
      "idNumber": 42,
      "performative": 3,
      "performativeName": "INFORM",
      "sender": "sender@jade-main",
      "receiver": "test-agent1@jade-main",
      "content": "(inform :content \"hello\")",
      "language": "FIPA/SL",
      "ontology": "FIPA-Agent-Management",
      "conversationId": "conv-1",
      "replyWith": "rw-1",
      "inReplyTo": "",
      "encoding": "Base64",
      "envelope": {
        "payloadEncoding": "Base64",
        "payload": "..."
      }
    },
    {
      "id": "msg-1002",
      "timestamp": "2024-01-15T10:30:01Z",
      "direction": "OUTGOING",
      "idNumber": 43,
      "performative": 16,
      "performativeName": "REQUEST",
      ...
    }
  ],
  "total": 150
}
```

### 3.2 Get Single Trace Message
- **Endpoint**: `GET /api/testagent/{agent}/trace/{messageId}`
- **Input**: path params `agent`, `messageId`
- **Output (200)**: full ACLMessage object (same structure as trace item above)
- **Error (404)**: `{"error": "Message not found in trace"}`

### 3.3 Delete Single Trace Message
- **Endpoint**: `DELETE /api/testagent/{agent}/trace/{messageId}`
- **Input**: path params `agent`, `messageId`
- **Output (200)**: `{"message": "Message removed from trace"}`
- **Error (404)**: `{"error": "Message not found in trace"}`

### 3.4 Clear Entire Trace
- **Endpoint**: `DELETE /api/testagent/{agent}/trace`
- **Input**: path param `agent`, optional query param `confirm=true`
- **Output (200)**: `{"message": "Trace cleared"}`

### 3.5 Get Message Statistics
- **Endpoint**: `GET /api/testagent/{agent}/trace/stats`
- **Input**: path param `agent`
- **Output (200)**:
```json
{
  "totalMessages": 150,
  "incoming": 85,
  "outgoing": 65,
  "byPerformative": {
    "INFORM": 45,
    "REQUEST": 30,
    "AGREE": 20,
    "NOT-UNDERSTOOD": 5
  },
  "firstTimestamp": "2024-01-15T10:00:00Z",
  "lastTimestamp": "2024-01-15T10:30:01Z"
}
```

---

## 4. Message File I/O

### 4.1 Save Message to File
- **Endpoint**: `POST /api/testagent/{agent}/trace/{messageId}/save`
- **Input**: path params `agent`, `messageId`, body:
```json
{
  "filename": "/tmp/agent-message.acl"
}
```
- **Output (200)**: `{"message": "Message saved to /tmp/agent-message.acl"}`

### 4.2 Open Message from File
- **Endpoint**: `POST /api/testagent/{agent}/compose/open`
- **Input**:
```json
{
  "filename": "/tmp/saved-message.acl"
}
```
- **Output (200)**:
```json
{
  "message": "Message loaded from file",
  "message": { ...full ACLMessage... }
}
```
- **Error (404)**: `{"error": "File not found"}`

### 4.3 Save Message Queue/Trace to File
- **Endpoint**: `POST /api/testagent/{agent}/trace/export`
- **Input**:
```json
{
  "filename": "/tmp/message-trace.txt",
  "filters": {
    "direction": "INCOMING",
    "performative": "INFORM"
  }
}
```
- **Output (200)**:
```json
{
  "message": "Trace exported to /tmp/message-trace.txt",
  "count": 85
}
```

### 4.4 Read Message Queue from File
- **Endpoint**: `POST /api/testagent/{agent}/trace/import`
- **Input**:
```json
{
  "filename": "/tmp/saved-trace.txt"
}
```
- **Output (200)**:
```json
{
  "message": "Trace loaded",
  "count": 100,
  "messages": [...]
}
```

---

## 5. Message Templates

### 5.1 List Built-in Templates
- **Endpoint**: `GET /api/testagent/templates`
- **Input**: none
- **Output (200)**:
```json
{
  "templates": [
    {
      "name": "Simple INFORM",
      "performative": "INFORM",
      "contentExample": "(inform :content \"hello world\")"
    },
    {
      "name": "FIPA Request",
      "performative": "REQUEST",
      "contentExample": "(request :action (inform ...))"
    },
    {
      "name": "CFP (Call For Proposal)",
      "performative": "CFP",
      "contentExample": "(cfp :item (item ...))"
    },
    {
      "name": "Propose",
      "performative": "PROPOSE",
      "contentExample": "(propose :content \"I can do this\")"
    }
  ]
}
```

### 5.2 Apply Template to Compose Message
- **Endpoint**: `POST /api/testagent/{agent}/compose/template/{templateName}`
- **Input**: path params `agent`, `templateName`
- **Output (200)**:
```json
{
  "message": "Template applied",
  "composeMessage": {
    "performative": "INFORM",
    "content": "(inform :content \"hello world\")"
  }
}
```
- **Error (404)**: `{"error": "Unknown template: invalid-template"}`

---

## Summary of REST Endpoints (Test Agent)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tools/testagent/start` | Start Test Agent |
| GET | `/api/tools/testagent/status` | Get status |
| DELETE | `/api/tools/testagent` | Kill Test Agent |
| GET | `/api/testagent/performatives` | Get available performatives |
| GET | `/api/testagent/templates` | Get built-in message templates |
| GET | `/api/testagent/{agent}/compose` | Get current compose message |
| PUT | `/api/testagent/{agent}/compose` | Set compose message |
| POST | `/api/testagent/{agent}/compose/new` | Create new empty message |
| POST | `/api/testagent/{agent}/compose/send` | Send current message |
| POST | `/api/testagent/{agent}/compose/reply/{messageId}` | Create reply to received message |
| POST | `/api/testagent/{agent}/compose/open` | Open message from file |
| POST | `/api/testagent/{agent}/compose/template/{templateName}` | Apply a template to compose message |
| GET | `/api/testagent/{agent}/trace` | Get message trace |
| GET | `/api/testagent/{agent}/trace/{messageId}` | Get single trace message |
| DELETE | `/api/testagent/{agent}/trace/{messageId}` | Delete single trace message |
| DELETE | `/api/testagent/{agent}/trace` | Clear entire trace |
| GET | `/api/testagent/{agent}/trace/stats` | Get message statistics |
| POST | `/api/testagent/{agent}/trace/{messageId}/save` | Save message to file |
| POST | `/api/testagent/{agent}/trace/export` | Export (filtered) trace to file |
| POST | `/api/testagent/{agent}/trace/import` | Import trace from file |
