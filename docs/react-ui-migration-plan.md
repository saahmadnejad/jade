# JADE React UI Migration & Modernization Plan

## Overview
This document tracks the end-to-end plan to port 100% of the legacy Swing-based JADE GUI functionality to the modern React web UI (`frontend/apps/webapp`), while preserving the legacy Swing UI in `io.donbee.jade.tools.*` as the functional source of truth and comparison benchmark.

---

## Architecture & Guiding Principles

1. **Source of Truth (Zero-Destruction)**:
   - The legacy Swing GUI classes (`rma`, `dfgui`, `sniffer`, `DummyAgent`, `TestAgent`, `Introspector`, `LogManagerAgent`, `AgentTree`) remain untouched in `backend/jade/src/main/java/io/donbee/jade/tools/` and `io.donbee.jade.gui/`.
   - The platform can be launched with `-gui -rest-port 8080` to run both the legacy Swing interface and the Vert.x REST API + React UI concurrently for side-by-side verification.

2. **Modern & Comfortable UX**:
   - Replace free-text typing with **rich dropdowns, autocompletes, and select boxes** (e.g. Container selectors populated from live containers, Agent AID selectors populated from active agents, Performative dropdowns for all 22 FIPA speech acts, Language & Ontology selectors).
   - Unified **Hierarchical Platform Tree View** (`AgentTree` parity) with real-time status chips and action menus directly on nodes (Kill, Suspend, Resume, Move, Clone, Freeze, Thaw, Send Message).
   - Interactive **ACL Message Composer & Tester** (unifying and modernizing DummyAgent & TestAgent) with message templates, header slots, and one-click Reply.
   - **Visual Message Sequence Flow** (Sniffer canvas parity) rendering agent lifelines and performative arrows alongside tabular history.
   - Live synchronization via WebSockets (`/api/messages/stream`) and periodic status polling.

3. **Backend Standards**:
   - Every blocking JADE API call is executed asynchronously via `ctx.vertx().executeBlocking(...)`.
   - Comprehensive error payloads `{"error": "...", "code": ...}` handled gracefully in React with snackbars.
   - JavaDoc on every handler detailing `<b>Old GUI implementation</b>`.

---

## Detailed Implementation Phases

### Phase 1: API Audit & Backend Extensions
- [x] Audit all existing 46 REST endpoints and ensure adherence to contract & tests.
- [ ] **Platform Tree API (`GET /api/platform/tree`)**:
  - Exposes nested JSON tree: Platform -> Containers -> Agents (including state, isMain, addresses, ownership).
  - Handler: `PlatformTreeHandler.java`.
- [ ] **Message Send API (`POST /api/messages/send`)**:
  - Allows injecting ACL messages into the JADE platform from REST.
  - Supports all FIPA performatives, sender AID, receivers, reply-to, content, language, ontology, protocol, conversation-id, reply-with, in-reply-to, and user properties.
  - Handler: `MessageSendHandler.java`.

### Phase 2: Modern Shared Components & Comfortable UX
- [ ] **Platform Tree View (`PlatformTreeView.tsx`)**:
  - Hierarchical MUI TreeView with container nodes and agent leaf nodes.
  - Live status badges: `ACTIVE` (green), `SUSPENDED` (warning), `FROZEN` (info).
  - Context menu / Action menu on nodes: Quick actions for Kill, Suspend, Resume, Clone, Move, Freeze, Thaw, Send Message.
- [ ] **Smart Form Selectors**:
  - Enhance Agent Deploy Dialog: Container dropdown populated from `/api/containers`, Class suggestions.
  - Enhance Move / Clone / Freeze / Thaw Dialogs: Destination container dropdowns.
  - Enhance DF Dialogs: Agent AID autocomplete populated from active platform agents.
- [ ] **About JADE Dialog (`AboutDialog.tsx`)**:
  - Modal displaying JADE version, revision, JVM platform info, active containers, active agents count, and credits.

### Phase 3: Interactive ACL Message Composer & Queue (DummyAgent / TestAgent Parity)
- [ ] **Message Composer Modal & Page (`MessageComposerDialog.tsx`)**:
  - Performative dropdown (all 22 FIPA performatives: INFORM, REQUEST, CFP, PROPOSE, AGREE, REFUSE, etc.).
  - Receiver autocomplete selector populated from active platform agents.
  - Slotted protocol fields: Conversation-ID, Protocol, Ontology, Language, Reply-With, In-Reply-To.
  - Message templates selector (e.g. FIPA-Request, FIPA-Query, Propose, Inform).
  - Content payload editor with formatting.
  - Quick action: "Send Message".
- [ ] **Message Queues & Reply Flow**:
  - Recent messages history table with one-click "Reply" button that pre-populates sender/receiver inversion and `in-reply-to`.

### Phase 4: Sniffer Enhancements (Visual Message Sequence Flow)
- [ ] **Visual Sequence Flow Component (`SequenceDiagramView.tsx`)**:
  - Agent lifelines / columns.
  - Directional message arrows connecting sender to receiver with performative chips.
  - Click on arrow to inspect full ACL message envelope.
  - Performative filter dropdown and agent filter.
  - Clear canvas and Export/Download trace as JSON.

### Phase 5: Verification & Parity Testing
- [ ] Vitest unit & integration tests for all new React components and pages.
- [ ] JUnit integration tests for all new REST endpoints in `RestAPIIntegrationTest.java`.
- [ ] Dual-run manual verification: Run platform with `-gui` and test React actions side-by-side with Swing GUI.
