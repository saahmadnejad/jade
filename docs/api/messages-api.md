# Messages API

Live visibility into the ACL message traffic of the platform. Captures every
ACL message **dispatched by any in-process container** (Main Container and
scenario containers, which live in the same JVM) at the messaging-service send
point (the closest REST-era equivalent of the old Sniffer tool).

> **Capture scope:** `MessageTrafficMonitor` uses static JVM-wide listeners
> fired in `MessagingService.CommandSourceSink`, so every container in the
> platform process is covered, including each scenario instance's dedicated
> container. Agents on truly remote (out-of-process) platforms are not
> captured.

All responses are JSON. Errors always follow the global format:
`{"error": "message", "code": <status>}`.

---

## GET /api/messages/recent

Returns the most recent captured messages, oldest first. Backed by an
in-memory ring buffer (default capacity 500).

### Query parameters

| Name   | Type | Required | Description                                        |
|--------|------|----------|----------------------------------------------------|
| `limit`| int  | no       | Max number of messages returned (default 100, max = buffer capacity) |
| `from` | text | no       | Filter: only messages whose sender local-name contains this substring |
| `to`   | text | no       | Filter: only messages whose receiver local-name contains this substring |

### Response — 200 OK

```json
{
  "messages": [
    {
      "id": "42",
      "timestamp": "2026-08-25T10:15:30.123Z",
      "sender": "team-demo-1-manager",
      "receiver": "team-demo-1-architect",
      "performative": "request",
      "protocol": "fipa-request",
      "ontology": "dev-team-ontology",
      "content": "(task architect DESIGN ...)"
    }
  ],
  "total": 1,
  "dropped": 0
}
```

Field notes:

- `id`: monotonic sequence number of the capture (per platform run).
- `sender` / `receiver`: agent local names.
- `content`: **truncated to 256 characters** in list/stream views — fetch the
  full message via `GET /api/messages/{id}`.
- `dropped`: number of older messages evicted from the ring buffer since startup.

---

## GET /api/messages/{id}

Returns one captured message with its **full, untruncated content**. Same
schema as a list element. `404` when the id is unknown or was evicted from
the buffer.

```json
{
  "id": "42",
  "timestamp": "2026-08-25T10:15:30.123Z",
  "sender": "team-demo-1-manager",
  "receiver": "team-demo-1-architect",
  "performative": "request",
  "protocol": "fipa-request",
  "ontology": "dev-team-ontology",
  "content": "... complete, untruncated content ..."
}
```

---

## WS /api/messages/stream

WebSocket endpoint that pushes each newly captured message as a JSON frame with
the same schema as one element of the `messages` array above:

```json
{
  "id": "43",
  "timestamp": "2026-08-25T10:15:31.456Z",
  "sender": "team-demo-1-implementer",
  "receiver": "team-demo-1-tester",
  "performative": "request",
  "protocol": "fipa-request",
  "ontology": "dev-team-ontology",
  "content": "(peer-task tester TEST-REPORT ...)"
}
```

Behaviour:

- One frame per captured message; frames arrive in capture order.
- The server sends nothing else (no heartbeats); clients may close at any time.
- When a client disconnects its subscription is removed automatically.

### Client-side filtering

The stream is unfiltered; clients filter locally (e.g. by sender/receiver) or
re-fetch `/api/messages/recent?from=...&to=...`.

---

## Old GUI equivalent

This API replaces the live message views of the old Swing tools — most closely
the Sniffer's agent/message canvas (`io.donbee.jade.tools.sniffer`) and the
Introspector's message list (`io.donbee.jade.tools.introspector.gui.MessagePanel`).
The React frontend consumes these endpoints on its **MessagesPage**.
