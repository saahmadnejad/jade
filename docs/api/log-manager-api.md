# Log Manager REST API Specification

**Purpose:** Specification for the Log Manager tool's REST endpoints. This is the work queue for implementing the logging management backend API — logger configuration, container-level log level management, and file load/save operations. Update as endpoints are implemented.

Detailed input/output specifications for Log Manager Agent functionalities.

The Log Manager agent allows configuring the logging system on the platform and individual containers. It manages per-container log displays and log levels.

---

## 1. Log Manager Lifecycle

### 1.1 Start Log Manager Agent
- **Endpoint**: `POST /api/tools/logger/start`
- **Input**:
```json
{
  "container": "Main-Container"
}
```
- **Output (201)**:
```json
{
  "message": "Log Manager started",
  "agent": "logger@jade-main",
  "container": "Main-Container"
}
```
- **Error (400)**: `{"error": "Log Manager already running"}`
- **Error (404)**: `{"error": "Container not found"}`
- **JADE backend call**: AMS `CreateAgent` for `io.donbee.jade.tools.logging.LogManagerAgent`

### 1.2 Get Log Manager Status
- **Endpoint**: `GET /api/tools/logger/status`
- **Input**: none
- **Output (200)**:
```json
{
  "running": true,
  "agent": "logger@jade-main",
  "container": "Main-Container",
  "defaultLogSystem": "java.util.logging",
  "managedContainers": ["Main-Container", "Node1-Container"]
}
```
- **Output (200, not running)**: `{"running": false}`

### 1.3 Exit Log Manager
- **Endpoint**: `DELETE /api/tools/logger`
- **Input**: none
- **Output (200)**: `{"message": "Log Manager agent deleted"}`
- **JADE backend call**: AMS `KillAgent` on logger

---

## 2. Logging System Configuration

### 2.1 Get Default Logging System
- **Endpoint**: `GET /api/logger/system`
- **Input**: none
- **Output (200)**:
```json
{
  "defaultLogSystem": "java.util.logging",
  "availableSystems": ["java.util.logging", "log4j"]
}
```
- **JADE backend call**: Read `LogManager` default implementation class

### 2.2 Set Default Logging System
- **Endpoint**: `PUT /api/logger/system`
- **Input**:
```json
{
  "system": "java.util.logging"
}
```
- **Output (200)**: `{"message": "Default logging system set to 'java.util.logging'"}`
- **Error (400)**: `{"error": "Unknown logging system 'invalid'"}`
- **JADE backend call**: Update `LogManager.defaultLogManager` class

### 2.3 Get Available Logging Systems
- **Endpoint**: `GET /api/logger/systems`
- **Input**: none
- **Output (200)**:
```json
{
  "systems": [
    {
      "name": "java.util.logging",
      "className": "io.donbee.jade.tools.logging.JavaLoggingLogManagerImpl"
    },
    {
      "name": "log4j",
      "className": "io.donbee.jade.tools.logging.Log4jLogManagerImpl"
    }
  ]
}
```

---

## 3. Container Log Management

### 3.1 Start Managing Log on Container
- **Endpoint**: `POST /api/logger/containers/{container}/manage`
- **Input**: path param `container`, optional body:
```json
{
  "level": "INFO"
}
```
- **Output (200)**:
```json
{
  "message": "Log management started on container 'Main-Container'",
  "logWindow": "container-Main-Container@jade-main"
}
```
- **Error (404)**: `{"error": "Container not found"}`
- **Error (400)**: `{"error": "Log management already active on this container"}`
- **JADE backend call**: Open a log management channel / create LogManagerAgent on the container

### 3.2 Stop Managing Log on Container
- **Endpoint**: `DELETE /api/logger/containers/{container}/manage`
- **Input**: path param `container`
- **Output (200)**: `{"message": "Log management stopped on container 'Main-Container'"}`
- **JADE backend call**: Close log management channel on the container

### 3.3 Get Managed Containers
- **Endpoint**: `GET /api/logger/containers`
- **Input**: none
- **Output (200)**:
```json
{
  "managedContainers": [
    {
      "name": "Main-Container",
      "logLevel": "INFO",
      "messageCount": 450
    },
    {
      "name": "Node1-Container",
      "logLevel": "FINE",
      "messageCount": 230
    }
  ]
}
```
- **JADE backend call**: Query LogManager for managed containers list

---

## 4. Log Level Management

### 4.1 Get All Loggers on a Container
- **Endpoint**: `GET /api/logger/containers/{container}/loggers`
- **Input**: path param `container`
- **Output (200)**:
```json
{
  "container": "Main-Container",
  "loggers": [
    {
      "name": "io.donbee.jade.core",
      "level": "INFO"
    },
    {
      "name": "io.donbee.jade.tools.rma",
      "level": "FINE"
    },
    {
      "name": "io.donbee.jade.domain",
      "level": "WARNING"
    }
  ]
}
```
- **Error (404)**: `{"error": "Container not found or log management not active"}`
- **JADE backend call**: `LogManager.getAllLogInfo()` for the container

### 4.2 Set Logger Level
- **Endpoint**: `PUT /api/logger/containers/{container}/loggers/{loggerName}`
- **Input**: path params `container`, `loggerName`, body:
```json
{
  "level": "FINE"
}
```
- **Output (200)**: `{"message": "Logger 'io.donbee.jade.core' level set to 'FINE'"}`
- **JADE backend call**: `LogManager.setLogLevel(loggerName, level)`

### 4.3 Get Available Log Levels
- **Endpoint**: `GET /api/logger/levels`
- **Input**: none
- **Output (200)**:
```json
{
  "levels": [
    {"name": "SEVERE", "value": 1000},
    {"name": "WARNING", "value": 900},
    {"name": "INFO", "value": 800},
    {"name": "FINE", "value": 700},
    {"name": "FINER", "value": 600},
    {"name": "FINEST", "value": 500},
    {"name": "OFF", "value": Integer.MAX_VALUE},
    {"name": "ALL", "value": Integer.MIN_VALUE}
  ]
}
```
- **JADE backend call**: `LogManager.getLogLevels()`

---

## 5. Log Message Stream

### 5.1 Get Container Log Messages
- **Endpoint**: `GET /api/logger/containers/{container}/messages`
- **Input**: path param `container`, optional query params:
  - `limit` — max messages (default 500)
  - `offset` — pagination offset
  - `level` — filter by log level
- **Output (200)**:
```json
{
  "container": "Main-Container",
  "messages": [
    {
      "timestamp": "2024-01-15T10:30:00Z",
      "level": "INFO",
      "logger": "io.donbee.jade.core.AgentContainer",
      "message": "Agent 'rma@jade-main' started"
    },
    {
      "timestamp": "2024-01-15T10:30:01Z",
      "level": "FINE",
      "logger": "io.donbee.jade.core.behaviours.ThreadedBehaviourScheduler",
      "message": "Behaviour step executed"
    }
  ],
  "total": 2300
}
```
- **JADE backend call**: Stream captured log messages from container's log handler

### 5.2 Clear Container Log
- **Endpoint**: `DELETE /api/logger/containers/{container}/messages`
- **Input**: path param `container`
- **Output (200)**: `{"message": "Log messages cleared for 'Main-Container'"}`
- **JADE backend call**: Clear the log message buffer for the container

### 5.3 Get Real-Time Log Stream (SSE)
- **Endpoint**: `GET /api/logger/containers/{container}/messages/stream`
- **Input**: path param `container`
- **Output**: Server-Sent Events stream of log messages
```
event: log
data: {"timestamp":"2024-01-15T10:30:00Z","level":"INFO","logger":"...","message":"..."}
```
- **JADE backend call**: Continuous log event stream via Vert.x event bus

---

## 6. Container Tree

### 6.1 Get Platform Container Tree
- **Endpoint**: `GET /api/logger/tree`
- **Input**: none
- **Output (200)**:
```json
{
  "containers": [
    {
      "name": "Main-Container",
      "address": "127.0.0.1:1099",
      "isMain": true,
      "logManaged": true
    },
    {
      "name": "Node1-Container",
      "address": "127.0.0.1:1098",
      "isMain": false,
      "logManaged": false
    }
  ]
}
```
- **JADE backend call**: AMS subscription to container lifecycle events

---

## 7. File Handling

### 7.1 Set Log File Handler for Logger
- **Endpoint**: `PUT /api/logger/containers/{container}/loggers/{loggerName}/file`
- **Input**:
```json
{
  "filename": "/var/log/jade/container.log",
  "pattern": "%h/jade-%u.log",
  "limit": 50000,
  "count": 5,
  "append": true
}
```
- **Output (200)**: `{"message": "File handler configured for logger 'io.donbee.jade.core'"}`
- **JADE backend call**: `LogManager.setFile(loggerName, fileHandlerConfig)`

### 7.2 Get Log Files on Container
- **Endpoint**: `GET /api/logger/containers/{container}/files`
- **Input**: path param `container`
- **Output (200)**:
```json
{
  "files": [
    {
      "path": "/var/log/jade/container.log",
      "size": 1048576,
      "lastModified": "2024-01-15T10:00:00Z"
    }
  ]
}
```

---

## Summary of REST Endpoints (Log Manager)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tools/logger/start` | Start Log Manager agent |
| GET | `/api/tools/logger/status` | Get Log Manager status |
| DELETE | `/api/tools/logger` | Kill Log Manager agent |
| GET | `/api/logger/system` | Get default logging system |
| PUT | `/api/logger/system` | Set default logging system |
| GET | `/api/logger/systems` | Get available logging systems |
| GET | `/api/logger/tree` | Get container tree |
| POST | `/api/logger/containers/{container}/manage` | Start log management on container |
| DELETE | `/api/logger/containers/{container}/manage` | Stop log management |
| GET | `/api/logger/containers` | Get list of managed containers |
| GET | `/api/logger/containers/{container}/loggers` | Get all loggers on container |
| PUT | `/api/logger/containers/{container}/loggers/{loggerName}` | Set logger level |
| GET | `/api/logger/levels` | Get available log levels |
| GET | `/api/logger/containers/{container}/messages` | Get container log messages |
| DELETE | `/api/logger/containers/{container}/messages` | Clear container log messages |
| GET | `/api/logger/containers/{container}/messages/stream` | SSE real-time log stream |
| PUT | `/api/logger/containers/{container}/loggers/{loggerName}/file` | Set log file handler |
| GET | `/api/logger/containers/{container}/files` | Get log files on container |
