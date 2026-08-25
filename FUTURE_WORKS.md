# Future Works

## Vert.x Integration Roadmap

### Phase 1: REST API Layer (current focus)
- Add Vert.x as a dependency
- Embed a Vert.x HTTP server in `Boot.java` to expose `/api/` endpoints
- Endpoints: platform status, containers, agents, DF, AMS
- Provides HTTP endpoints for the React frontend to communicate with JADE

### Phase 2: Event Bus Bridge
- Expose the Vert.x Event Bus over HTTP/WebSocket
- Allow the React frontend to subscribe to platform events in real-time

### Phase 3: Gradual IMTP Migration
- Replace JADE's internal RMI-based IMTP with Vert.x Event Bus
- Container-to-container communication via Vert.x event bus

### Phase 4: Full Vert.x Platform
- Make Vert.x the sole communication layer
- Keep FIPA protocols and ontologies intact

## Other Future Works

### Frontend Modernization
- Build full Jade UI pages (agent management, container view, DF browser, sniffer)
- Connect React frontend to the new Vert.x REST API
- Add real-time event streaming via WebSocket *(done for ACL message traffic: MessagesPage via `WS /api/messages/stream`)*

### Backend Modernization
- Migrate deprecated Java APIs (Integer constructor, Boolean constructor, etc.)
- Replace finalize() usage in JarClassLoader
- Address value-based class synchronization warnings
- Extend live message capture beyond the Main Container (currently only messages dispatched by agents hosted on the Main Container are reported; see `docs/api/messages-api.md`)
