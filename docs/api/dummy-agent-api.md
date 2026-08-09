# Dummy Agent REST API Specification

Detailed input/output specifications for Dummy Agent functionalities.

The Dummy Agent provides a simple GUI for composing and sending ACL messages manually. The REST API exposes message composition, sending, queueing, and file I/O operations.

---

## 1. Dummy Agent Lifecycle

### 1.1 Start Dummy Agent
- **Endpoint**: `POST /api/tools/dummy/start`
- **Input**:
```json
{
  "container": "Main-Container",
  "name": "dummy1"
}
```
- **Output (201)**:
```json
{
  "message": "Dummy Agent started",
  "agent": "dummy1@jade-main",
  "container": "Main-Container"
}
```
- **Error (400)**: `{"error": "Dummy Agent already running"}`
- **JADE backend call**: AMS `CreateAgent` for `io.donbee.jade.tools.DummyAgent.DummyAgent`

### 1.2 Get Dummy Agent Status
- **Endpoint**: `GET /api/tools/dummy/status`
- **Input**: none
- **Output (200)**:
```json
{
  "running": true,
  "agent": "dummy1@jade-main",
  "container": "Main-Container"
}
```

---

## 2. Current Message Management

### 2.1 Get Current Message
- **Endpoint**: `GET /api/dummy/{agent}/message`
- **Input**: path param `agent`
- **Output (200)**:
```json
{
  "message": {
    "performative": 15,
    "performativeName": "ACCEPT-PERFORMATIVE",
    "sender": "dummy1@jade-main",
    "receivers": [],
    "replyTo": [],
    "content": "",
    "language": "FIPA/SL",
    "ontology": "FIPA-Agent-Management",
    "protocol": null,
    "conversationId": "",
    "replyWith": "",
    "inReplyTo": "",
    "encoding": "Base64",
    "envelope": {
      "payloadEncoding": "Base64"
    }
  }
}
```

### 2.2 Set/Update Current Message
- **Endpoint**: `PUT /api/dummy/{agent}/message`
- **Input**:
```json
{
  "performative": "INFORM",
  "sender": "dummy1@jade-main",
  "receivers": ["receiver1@jade-main", "receiver2@jade-main"],
  "replyTo": ["reply-to@jade-main"],
  "content": "(inform :sender dummy1 :receiver receiver1 :content \"Hello World\")",
  "language": "FIPA/SL",
  "ontology": "FIPA-Agent-Management",
  "protocol": null,
  "conversationId": "conv-123",
  "replyWith": "reply-456",
  "inReplyTo": "",
  "encoding": "Base64"
}
```
- **Output (200)**: `{"message": "Current message updated"}`
- **Error (400)**: `{"error": "Invalid performative"}` — must be valid FIPA performative

### 2.3 Reset Current Message
- **Endpoint**: `POST /api/dummy/{agent}/message/reset`
- **Input**: none
- **Output (200)**:
```json
{
  "message": "Current message reset",
  "message": {
    "performative": 15,
    "performativeName": "ACCEPT-PERFORMATIVE",
    "sender": "dummy1@jade-main",
    "receivers": [],
    "content": ""
  }
}
```
- **JADE backend call**: Create a new empty ACLMessage

### 2.4 Send Current Message
- **Endpoint**: `POST /api/dummy/{agent}/message/send`
- **Input**: none (sends the current message)
- **Output (200)**:
```json
{
  "message": "Message sent",
  "aclMessage": {
    "id": 42,
    "performative": "INFORM",
    "sender": "dummy1@jade-main",
    "receivers": ["receiver1@jade-main"]
  }
}
```
- **Error (400)**: `{"error": "No receivers specified"}`

### 2.5 Send Message to Specific Recipients
- **Endpoint**: `POST /api/dummy/{agent}/message/send-to`
- **Input**:
```json
{
  "receivers": ["agent1@jade-main", "agent2@jade-main"]
}
```
- **Output (200)**:
```json
{
  "message": "Message sent to 2 recipients",
  "aclMessage": {...}
}
```

### 2.6 Get Available Performatives
- **Endpoint**: `GET /api/dummy/performatives`
- **Input**: none
- **Output (200)**:
```json
{
  "performatives": [
    "ACCEPT-PERFORMATIVE",
    "AGREE",
    "CANCEL",
    "CFPDT",
    "CHOKE-AGENT",
    "CONFIRM",
    "DISCONFIRM",
    "FAILURE",
    "FUGACITY",
    "INFORM",
    "INFORM-REF",
    "NOT-UNDERSTOOD",
    "PROPAGATE",
    "PROPOSE",
    "QUERY-IF",
    "QUERY-REF",
    "QUERY-SPLIT",
    "REFUSE",
    "REJECT-PROPOSAL",
    "RELEVANCE",
    "REQUEST",
    "REQUEST-WHEN",
    "REQUEST-WITHOUT",
    "SUBSCRIBE",
    "SULTAN-AGENT",
    "UPDATE",
    "NOT-UNDERSTOOD"
  ]
}
```

---

## 3. Queued Messages

### 3.1 List Queued Messages
- **Endpoint**: `GET /api/dummy/{agent}/queue`
- **Input**: path param `agent`, optional query param `direction` (`INCOMING`, `OUTGOING`, `ALL`)
- **Output (200)**:
```json
{
  "messages": [
    {
      "id": 1,
      "timestamp": "2024-01-15T10:30:00Z",
      "direction": "OUTGOING",
      "performative": "INFORM",
      "sender": "dummy1@jade-main",
      "receiver": "receiver1@jade-main",
      "content": "(inform ...)",
      "status": "SENT"
    },
    {
      "id": 2,
      "timestamp": "2024-01-15T10:30:01Z",
      "direction": "INCOMING",
      "performative": "AGREE",
      "sender": "receiver1@jade-main",
      "receiver": "dummy1@jade-main",
      "content": "(agree ...)",
      "status": "RECEIVED"
    }
  ]
}
```

### 3.2 Get Single Queued Message
- **Endpoint**: `GET /api/dummy/{agent}/queue/{id}`
- **Input**: path params `agent`, `id`
- **Output (200)**: full ACLMessage object with envelope
- **Error (404)**: `{"error": "Queued message not found"}`

### 3.3 Delete Queued Message
- **Endpoint**: `DELETE /api/dummy/{agent}/queue/{id}`
- **Input**: path params `agent`, `id`
- **Output (200)**: `{"message": "Message deleted from queue"}`
- **Error (404)**: `{"error": "Queued message not found"}`

### 3.4 Clear Queue
- **Endpoint**: `DELETE /api/dummy/{agent}/queue`
- **Input**: path param `agent`, optional query param `confirm=true`
- **Output (200)**: `{"message": "All queued messages cleared"}`

### 3.5 View Queued Message Details
- **Endpoint**: `GET /api/dummy/{agent}/queue/{id}/detail`
- **Input**: path params `agent`, `id`
- **Output (200)**: detailed message with parsed content, envelope info, addresses
- **JADE backend call**: Expand full ACLMessage with envelope

---

## 4. Message File I/O

### 4.1 Save Current Message to File
- **Endpoint**: `POST /api/dummy/{agent}/message/save`
- **Input**:
```json
{
  "filename": "/tmp/my-message.acl"
}
```
- **Output (200)**: `{"message": "Message saved to /tmp/my-message.acl"}`
- **Error (500)**: `{"error": "Failed to save message"}`

### 4.2 Open Message from File
- **Endpoint**: `POST /api/dummy/{agent}/message/open`
- **Input**:
```json
{
  "filename": "/tmp/my-message.acl"
}
```
- **Output (200)**:
```json
{
  "message": "Message loaded from file",
  "message": { ...full ACLMessage object... }
}
```
- **Error (404)**: `{"error": "File not found"}`

### 4.3 Save Message Queue to File
- **Endpoint**: `POST /api/dummy/{agent}/queue/save`
- **Input**:
```json
{
  "filename": "/tmp/message-queue.acl"
}
```
- **Output (200)**: `{"message": "Queue saved to /tmp/message-queue.acl", "count": 5}`

### 4.4 Open Message Queue from File
- **Endpoint**: `POST /api/dummy/{agent}/queue/open`
- **Input**:
```json
{
  "filename": "/tmp/message-queue.acl"
}
```
- **Output (200)**: `{"message": "Queue loaded", "messages": [...]}`

---

## 5. Reply Functionality

### 5.1 Create Reply to Queued Message
- **Endpoint**: `POST /api/dummy/{agent}/queue/{id}/reply`
- **Input**: path params `agent`, `id`
- **Output (200)**:
```json
{
  "message": "Reply created",
  "reply": {
    "performative": "INFORM",
    "sender": "dummy1@jade-main",
    "receivers": ["original-sender@jade-main"],
    "inReplyTo": "original-reply-with",
    "conversationId": "original-conv-id"
  }
}
```
- **JADE backend call**: `ACLMessage.createReply()` on the selected queued message

### 5.2 Set Message Properties
- **Endpoint**: `PATCH /api/dummy/{agent}/message`
- **Input**:
```json
{
  "language": "XML",
  "ontology": "Weather-Ontology",
  "encoding": "ISO-8859-1"
}
```
- **Output (200)**: `{"message": "Message properties updated"}`

---

## Summary of REST Endpoints (Dummy Agent)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tools/dummy/start` | Start Dummy Agent |
| GET | `/api/tools/dummy/status` | Get status |
| GET | `/api/dummy/performatives` | Get all available FIPA performatives |
| GET | `/api/dummy/{agent}/message` | Get current message |
| PUT | `/api/dummy/{agent}/message` | Set/update current message |
| POST | `/api/dummy/{agent}/message/reset` | Reset current message |
| POST | `/api/dummy/{agent}/message/send` | Send current message |
| POST | `/api/dummy/{agent}/message/send-to` | Send to specific recipients |
| PATCH | `/api/dummy/{agent}/message` | Update message properties |
| POST | `/api/dummy/{agent}/message/save` | Save message to file |
| POST | `/api/dummy/{agent}/message/open` | Open message from file |
| GET | `/api/dummy/{agent}/queue` | List queued messages |
| GET | `/api/dummy/{agent}/queue/{id}` | Get single queued message |
| DELETE | `/api/dummy/{agent}/queue/{id}` | Delete queued message |
| DELETE | `/api/dummy/{agent}/queue` | Clear entire queue |
| GET | `/api/dummy/{agent}/queue/{id}/detail` | Get detailed queued message |
| POST | `/api/dummy/{agent}/queue/{id}/reply` | Create reply to queued message |
| POST | `/api/dummy/{agent}/queue/save` | Save message queue to file |
| POST | `/api/dummy/{agent}/queue/open` | Open message queue from file |
