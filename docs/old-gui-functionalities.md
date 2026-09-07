# Old JADE GUI — Functionality Migration Tracker

This document lists every functionality available in the **old Swing-based JADE GUI tools** (the "old GUI"), organized by tool. Use the checkboxes to track migration progress into the new React GUI, and the testing checklist to verify each feature's Definition of Done (DoD).

The old GUI source lives in:
- `backend/jade/src/main/java/io/donbee/jade/tools/` (agent tool classes)
- `backend/jade/src/main/java/io/donbee/jade/gui/` (shared Swing components)

The new React GUI source lives in:
- `frontend/apps/webapp/src/`

---

## Legend

- **MIGRATED**: Functionality has been reimplemented in the new React GUI.
- **TESTED**: Functionality has been verified against the live REST API + backend behaviour.
- Each item has its own row for tracking.

---

## 1. RMA — Remote Management Agent

Package: `io.donbee.jade.tools.rma`  
Purpose: Main platform administration GUI — central hub for managing containers, agents, and launching tools.

### Menu: File
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.1 | Close RMA — detaches RMA agent from platform | [ ] | [ ] | |
| 1.2 | Exit RMA — kills RMA agent and closes window | [ ] | [ ] | |
| 1.3 | Shutdown Platform — shuts down entire JADE platform (with confirm dialog) | [x] | [x] | via AMS ShutdownPlatform |

### Menu: Actions — Agent Operations
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.4 | Start New Agent — create agent (name, class, container, owner, args) | [x] | [x] | via AMS CreateAgent |
| 1.5 | Kill — forcefully terminate a selected agent | [x] | [x] | via AMS KillAgent |
| 1.6 | Suspend Agent — suspend a running agent | [x] | [x] | via AMS Modify |
| 1.7 | Resume Agent — resume a suspended agent | [x] | [x] | via AMS Modify |
| 1.8 | Custom Agent — custom action on an agent | [ ] | [ ] | |
| 1.9 | Migrate Agent — move agent to another container | [x] | [x] | via MobilityOntology MoveAction |
| 1.10 | Clone Agent — clone agent to a new container | [x] | [x] | via MobilityOntology CloneAction |
| 1.11 | Save Agent — persist agent state to a repository | [x] | [x] | via PersistenceOntology SaveAgent |
| 1.12 | Load Agent — load agent from repository into container | [x] | [x] | via PersistenceOntology LoadAgent |
| 1.13 | Freeze Agent — freeze agent (suspend + serialize to buffer) | [x] | [x] | via PersistenceOntology FreezeAgent |
| 1.14 | Thaw Agent — thaw frozen agent back to live container | [x] | [x] | via PersistenceOntology ThawAgent |
| 1.15 | Change Agent Ownership — change ownership of an agent | [x] | [x] | via AMS Modify |
| 1.16 | Register Remote Agent with local AMS | [x] | [x] | via AMS Register |

### Menu: Actions — Container Operations
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.17 | Save Container — persist entire container state | [x] | [x] | via PersistenceOntology SaveContainer |
| 1.18 | Load Container — load container from repository | [x] | [x] | via PersistenceOntology LoadContainer |

### Menu: Tools — Tool Launch
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.19 | Start Sniffer — launch Sniffer tool on selected container | [x] | [x] | via ToolsPage |
| 1.20 | Start DummyAgent — launch Dummy Agent tool | [x] | [x] | via ToolsPage |
| 1.21 | Start LoggerAgent — launch Log Manager agent | [x] | [x] | via ToolsPage |
| 1.22 | Start IntrospectAgent — launch Introspector on selected agent | [x] | [x] | via ToolsPage |
| 1.23 | Show DFGui — open the Directory Facilitator GUI | [x] | [x] | via ToolsPage |

### Menu: Remote Platforms
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.24 | Add Platform via AMS AID — add remote platform via AMS contact | [x] | [x] | via PlatformsPage |
| 1.25 | Add Platform via URL — add remote platform by fetching AP description URL | [x] | [x] | via PlatformsPage fetch |
| 1.26 | View AP Description — view platform description of remote platform | [x] | [x] | via PlatformsPage |
| 1.27 | Refresh AP Description — refresh platform description | [x] | [x] | via PlatformsPage |
| 1.28 | Remove Remote Platform — remove platform from tree | [x] | [x] | via PlatformsPage |
| 1.29 | Refresh Agent List — refresh agent list for remote platform | [x] | [x] | via PlatformsPage |

### Menu: MTP Management
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.30 | Manage Installed MTPs — manage message transport protocols | [x] | [x] | via ContainersPage MTP tab |
| 1.31 | Install a new MTP — install MTP on a container | [x] | [x] | via AMS InstallMTP |
| 1.32 | Uninstall an MTP — uninstall MTP from container | [x] | [x] | via AMS UninstallMTP |

### Tree View (AgentTree)
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.33 | Platform container tree display (platform → containers → agents) | [ ] | [ ] | |
| 1.34 | Drag-and-drop ACL message files onto agents to send | [ ] | [ ] | |
| 1.35 | Popup menu on agents: Start, Kill, Suspend, Resume, Clone, Move, Freeze, Thaw, Save, Load, Inspect, Sniff, Show DF GUI | [ ] | [ ] | |
| 1.36 | Popup menu on containers: Save, Load, Kill, Install/Uninstall MTPs | [ ] | [ ] | |
| 1.37 | Popup menu on remote platforms: View, Refresh, Remove | [ ] | [ ] | |

### Toolbar
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.38 | Toolbar buttons: Start, Kill, Suspend, Resume, Custom, Move, Clone, Save, Load, Freeze, Thaw, Sniffer, DummyAgent, LoggerAgent, Introspector, Add Remote Platform | [ ] | [ ] | |

### Menu: Help
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 1.39 | About JADE — shows the JADE about dialog | [ ] | [ ] | |

---

## 2. Sniffer — ACL Message Interception

Package: `io.donbee.jade.tools.sniffer`  
Purpose: Intercepts and displays all ACL messages exchanged between agents on the platform.

### Menu: Actions
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 2.1 | Do Sniff — start sniffing messages from selected agents | [ ] | [ ] | |
| 2.2 | Do Not Sniff — stop sniffing selected agents | [ ] | [ ] | |
| 2.3 | Show Only — show messages only from selected agents (filtered) | [ ] | [ ] | |
| 2.4 | Clear Canvas — clears all displayed messages and agents | [ ] | [ ] | |
| 2.5 | Display Log File — display a previously saved sniffer log | [ ] | [ ] | |
| 2.6 | Write Log File — save all sniffed messages to a log file | [ ] | [ ] | |
| 2.7 | Write Message List — save message list to a file | [ ] | [ ] | |
| 2.8 | Exit Sniffer — exits sniffer (unsniffs all, deletes itself) | [ ] | [ ] | |

### Agent Canvas (Graphical Message View)
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 2.9 | Visual message flow between agents (canvas with agent boxes + arrows) | [x] | [x] | replaced by live **MessagesPage** (`WS /api/messages/stream`, see `docs/api/messages-api.md`); canvas rendering not replicated, table view instead |
| 2.10 | Double-click messages to view full ACL content | [ ] | [ ] | |
| 2.11 | Right-click on messages: view sender/receiver, save message | [ ] | [ ] | |
| 2.12 | Right-click on agents: Do Sniff / Do Not Sniff this Agent | [ ] | [ ] | |

### Tree View
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 2.13 | Platform tree showing all containers and agents | [ ] | [ ] | |
| 2.14 | Visual differentiation of sniffed vs. non-sniffed agents | [ ] | [ ] | |

### Toolbar
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 2.15 | Toolbar buttons: Clear Canvas, Display Log File, Write Log File, Write Message List, Do Sniff, Do Not Sniff, Show Only, Exit | [ ] | [ ] | |

### Preload Configuration
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 2.16 | `sniffer.properties` preload file for auto-sniffing agents at startup | [ ] | [ ] | |
| 2.17 | Performatives-based message filtering in preload config | [ ] | [ ] | |

### Menu: About
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 2.18 | About JADE | [ ] | [ ] | |
| 2.19 | About Sniffer (version and authors) | [ ] | [ ] | |

---

## 3. DF GUI — Directory Facilitator

Package: `io.donbee.jade.tools.dfgui`  
Purpose: Visual tool for managing the Directory Facilitator (yellow pages service).

### Views (Tabbed)
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 3.1 | Registrations with this DF — table of agents registered with local DF | [x] | [x] | |
| 3.2 | Search Result — results of last search operation | [x] | [x] | |
| 3.3 | DF Federation — parent/child DF federation tables | [x] | [x] | |

### Menu: General
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 3.4 | Exit DF — kills the DF agent | [ ] | [ ] | |
| 3.5 | Close GUI — hides DF GUI (keeps DF running) | [ ] | [ ] | |

### Menu: Catalogue
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 3.6 | View — view full service description (DFA description) of selected agent | [x] | [x] | Modify dialog pre-fills from registration |
| 3.7 | Modify — modify description of registered agent | [x] | [x] | via DF modifyRegistration |
| 3.8 | Register — register a new agent with the DF (AID, addresses, services) | [x] | [x] | |
| 3.9 | Deregister — deregister selected agent from DF | [x] | [x] | |
| 3.10 | Search — search for agents (max depth, max results, description constraints) | [x] | [x] | |

### Menu: Super DF
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 3.11 | Federate — federate this DF with another DF | [x] | [x] | via api.df.federate |

### Toolbar
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 3.12 | Toolbar buttons: Exit, Close GUI, View, Modify, Deregister, Register, Search, Federate, About | [ ] | [ ] | |

### Keyboard Shortcuts
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 3.13 | Double-click on table row → View description | [ ] | [ ] | |
| 3.14 | Delete key on selected row → Deregister | [ ] | [ ] | |

### Menu: Help
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 3.15 | About DF | [ ] | [ ] | |
| 3.16 | About JADE | [ ] | [ ] | |

---

## 4. Introspector (Agent Debugger)

Package: `io.donbee.jade.tools.introspector`  
Purpose: Attaches to a target agent and allows debugging its execution (behaviours, messages, state).

### Menu: View
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 4.1 | View Messages — toggle incoming/outgoing message tables | [ ] | [ ] | |
| 4.2 | View Behaviours — toggle behaviour tree display | [ ] | [ ] | |

### Menu: State
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 4.3 | Kill — mark agent to be killed | [ ] | [ ] | not yet implemented in old GUI |
| 4.4 | Suspend — suspend the agent | [ ] | [ ] | not yet implemented in old GUI |
| 4.5 | WakeUp — wake up the agent | [ ] | [ ] | not yet implemented in old GUI |
| 4.6 | Wait — wait state | [ ] | [ ] | not yet implemented in old GUI |
| 4.7 | Go — let agent run freely | [ ] | [ ] | |

### Menu: Debug
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 4.8 | Step — advance agent one step (execute one behaviour action) | [ ] | [ ] | |
| 4.9 | Break — pause all behaviours | [ ] | [ ] | |
| 4.10 | Slow — slow execution mode with delays | [ ] | [ ] | |
| 4.11 | Go — resume normal execution | [ ] | [ ] | |

### Message Display (4 tables)
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 4.12 | Incoming Messages — Pending | [ ] | [ ] | |
| 4.13 | Incoming Messages — Received | [ ] | [ ] | |
| 4.14 | Outgoing Messages — Pending | [ ] | [ ] | |
| 4.15 | Outgoing Messages — Sent | [ ] | [ ] | |

### Behaviour Tree
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 4.16 | Hierarchical tree of all active behaviours | [ ] | [ ] | |
| 4.17 | Real-time updates as behaviours execute | [ ] | [ ] | |
| 4.18 | State panel showing agent state changes | [ ] | [ ] | |

### Popup Menus
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 4.19 | Popup on message tables | [ ] | [ ] | |
| 4.20 | Popup on tree nodes | [ ] | [ ] | |

### Menu: Help
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 4.21 | About JADE | [ ] | [ ] | |

---

## 5. Log Manager Agent

Package: `io.donbee.jade.tools.logging`  
Purpose: Management agent for configuring logging on the platform and individual containers.

### Menu: Settings
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 5.1 | Set Default Logging System — set the default logging implementation | [ ] | [ ] | |
| 5.2 | Exit — exit the Log Manager agent | [ ] | [ ] | |

### Menu: Logs
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 5.3 | Start Managing Log — start log management on selected container | [ ] | [ ] | |
| 5.4 | Stop Managing Log — stop log management on selected container | [ ] | [ ] | |

### Agent Tree
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 5.5 | Tree of all platform containers and agents | [ ] | [ ] | |
| 5.6 | Popup on containers: Start/Stop Managing Log | [ ] | [ ] | |

### Per-Container Log Windows
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 5.7 | Per-container log display in internal windows | [ ] | [ ] | |
| 5.8 | Real-time log message streaming | [ ] | [ ] | |
| 5.9 | Log level adjustment via LogManager interface | [ ] | [ ] | |

### Toolbar
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 5.10 | Toolbar buttons: Set Default Logging System, Start Managing Log, Stop Managing Log | [ ] | [ ] | |

---

## 6. Dummy Agent

Package: `io.donbee.jade.tools.DummyAgent`  
Purpose: Lightweight agent with a GUI for manually composing and sending ACL messages.

### Menu: General
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 6.1 | Exit — kill the Dummy Agent | [ ] | [ ] | |

### Menu: Current message
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 6.2 | Reset — reset current message to new empty ACCEPT_PROPOSAL | [ ] | [ ] | |
| 6.3 | Send — send current message to selected recipients | [ ] | [ ] | |
| 6.4 | Open — load an ACL message from a file | [ ] | [ ] | |
| 6.5 | Save — save current message to a file | [ ] | [ ] | |
| 6.6 | Reply — create a reply to selected queued message | [ ] | [ ] | |
| 6.7 | View — view full details of a queued message | [ ] | [ ] | |
| 6.8 | Delete — delete a message from the queue | [ ] | [ ] | |
| 6.9 | Set — set message parameters/properties | [ ] | [ ] | |
| 6.10 | Open Queue — open a saved message queue | [ ] | [ ] | |
| 6.11 | Save Queue — save the current message queue | [ ] | [ ] | |
| 6.12 | Write Queue — write message queue to disk | [ ] | [ ] | |

### Queued Messages List
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 6.13 | List of sent/received messages with timestamps and direction | [ ] | [ ] | |
| 6.14 | Visual distinction between outgoing and incoming messages | [ ] | [ ] | |

### ACL Message Editor (AclGui component)
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 6.15 | Performatives dropdown (ACCEPT_PROPOSAL, INFORM, REQUEST, etc.) | [ ] | [ ] | |
| 6.16 | Sender field | [ ] | [ ] | |
| 6.17 | Receiver/To field (AID autocomplete) | [ ] | [ ] | |
| 6.18 | Reply-to field | [ ] | [ ] | |
| 6.19 | Content field with syntax highlighting | [ ] | [ ] | |
| 6.20 | Language field selector | [ ] | [ ] | |
| 6.21 | Ontology field selector | [ ] | [ ] | |
| 6.22 | Protocol field selector | [ ] | [ ] | |
| 6.23 | Reply-with / In-reply-to / Conversation-id fields | [ ] | [ ] | |
| 6.24 | Envelope for encoding | [ ] | [ ] | |

---

## 7. Test Agent

Package: `io.donbee.jade.tools.testagent`  
Purpose: Test agent with a GUI for sending and receiving ACL messages, with message trace.

### Message Composition Panel (ACLPanel)
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 7.1 | Full ACL message editor (same AclGui component as Dummy Agent) | [ ] | [ ] | |
| 7.2 | Send button to dispatch messages | [ ] | [ ] | |
| 7.3 | Support for reply templates | [ ] | [ ] | |

### ACL Trace Panel (ACLTracePanel)
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 7.4 | Message trace/history view | [ ] | [ ] | |
| 7.5 | Incoming and outgoing message logs | [ ] | [ ] | |
| 7.6 | Right-click popup on messages: save, view, forward | [ ] | [ ] | |

### Menu (with toolbar icons)
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 7.7 | New — create new empty message | [ ] | [ ] | |
| 7.8 | Open — load message from file | [ ] | [ ] | |
| 7.9 | Save — save current message to file | [ ] | [ ] | |
| 7.10 | Send — send the composed message | [ ] | [ ] | |
| 7.11 | Reply — create reply to selected received message | [ ] | [ ] | |
| 7.12 | Read Queue — read messages from queue file | [ ] | [ ] | |
| 7.13 | Save Queue — save message queue to file | [ ] | [ ] | |
| 7.14 | View — view selected message details | [ ] | [ ] | |
| 7.15 | Delete — delete selected message | [ ] | [ ] | |
| 7.16 | Current — reset current message | [ ] | [ ] | |

### Message Templates
| # | Functionality | MIGRATED | TESTED | Notes |
|---|--------------|----------|--------|-------|
| 7.17 | Built-in template examples (INFORM, REQUEST, etc.) | [ ] | [ ] | |

---

## 8. Shared GUI Components

Package: `io.donbee.jade.gui`  
Purpose: Reusable Swing components used across multiple old GUI tools.

| # | Component | MIGRATED | TESTED | Notes |
|---|----------|----------|--------|-------|
| 8.1 | AclGui — reusable ACL message composition dialog/form | [ ] | [ ] | Used by Dummy Agent, Test Agent, RMA error dialog |
| 8.2 | AgentTree — tree widget of platform structure | [ ] | [ ] | Used by RMA, Sniffer, Log Manager, Introspector |
| 8.3 | AgentTreeModel — data model backing AgentTree | [ ] | [ ] | |
| 8.4 | GuiProperties — centralized icon/image resource manager | [ ] | [ ] | |
| 8.5 | JadeLogoButton — reusable JADE logo button for toolbars | [ ] | [ ] | |
| 8.6 | APDescriptionPanel — panel displaying APDescription | [ ] | [ ] | Used by RMA, DF GUI |
| 8.7 | AboutJadeAction — reusable "About JADE" action | [ ] | [ ] | Used by RMA, Sniffer, DF GUI, Introspector, Dummy Agent |
| 8.8 | StringDlg — generic string input dialog | [ ] | [ ] | |
| 8.9 | TimeChooserDialog — time selection dialog | [ ] | [ ] | |
| 8.10 | AIDGui — dialog for editing AID (agent identifier) fields | [ ] | [ ] | |
| 8.11 | DFAgentDscDlg — dialog for filling DF agent descriptions | [ ] | [ ] | |
| 8.12 | ServiceDscDlg — dialog for adding service descriptions | [ ] | [ ] | |
| 8.13 | SingleProperty / UserPropertyGui — property editing dialogs | [ ] | [ ] | |
| 8.14 | ConstraintDlg — search constraint dialog for DF searches | [ ] | [ ] | |
| 8.15 | VisualStringList / VisualAIDList / VisualAPServiceList / VisualServicesList — list editors | [ ] | [ ] | |
| 8.16 | ClassSelectionDialog — class selection dialog | [ ] | [ ] | |
| 8.17 | BrowserLauncher — opens web browser dialog | [ ] | [ ] | |
| 8.18 | MyFilterImage — image filter utility | [ ] | [ ] | |
| 8.19 | TreeHelp / TreeIconRenderer — tree rendering helpers | [ ] | [ ] | |
| 8.20 | NodeDescriptor — node descriptor for tree | [ ] | [ ] | |

---

## REST API Endpoints (Backend)

These are the backend REST endpoints available to the new React GUI for implementing the above functionality.

Package: `io.donbee.jade.rest`  
Source: `backend/jade/src/main/java/io/donbee/jade/rest/RestAPIVerticle.java`

| # | Endpoint | Method | Functionality | TESTED | Notes |
|---|----------|--------|--------------|--------|-------|
| 9.1 | `/api/health` | GET | Health check — returns `{"status":"ok"}` | [x] | |
| 9.2 | `/api/version` | GET | Platform version info (version, revision, date) | [x] | |
| 9.3 | `/api/platform` | GET | Platform metadata (ID, container name, isMain, AMS, default DF) | [x] | |
| 9.4 | `/api/agents` | GET | List agents (add `?detail=true` for state/ownership/addresses) | [x] | `detail` param added |
| 9.5 | `/api/containers` | GET | List all containers with addresses, ports, isMain | [x] | **NEW** endpoint |
| 9.6 | `/api/df/registrations` | GET/POST | List and register agents with DF | [x] | |
| 9.7 | `/api/df/registrations/:agentName` | GET/PUT/DELETE | View, modify, deregister a registration | [x] | |
| 9.8 | `/api/df/federation` | GET/POST/DELETE | Parent/child DF listing, federate, deregister | [x] | |
| 9.9 | `/api/platform/shutdown` | POST | Shutdown entire JADE platform | [x] | |
| 9.10 | `/api/agents/:name/clone` | POST | Clone an agent to another container | [x] | Via POST `/api/agents/clone` |
| 9.11 | `/api/agents/:name/move` | POST | Move an agent to another container | [x] | |
| 9.12 | `/api/agents/:name/save` | POST | Save agent state to a repository | [x] | |
| 9.13 | `/api/agents/load` | POST | Load agent from repository into a container | [x] | |
| 9.14 | `/api/agents/register-remote` | POST | Register a remote agent with local AMS | [x] | |
| 9.15 | `/api/containers/:name/save` | POST | Save container state to a repository | [x] | |
| 9.16 | `/api/containers/:name/load` | POST | Load container state from a repository | [x] | |
| 9.17 | `/api/containers/:name/mtps` | POST | Install MTP on a container | [x] | |
| 9.18 | `/api/containers/:name/mtps` | GET | List installed MTPs on a container | [x] | |
| 9.19 | `/api/containers/:name/mtps/:address` | DELETE | Uninstall MTP from a container | [x] | |
| 9.20 | `/api/tools/:tool/start` | POST | Start a GUI tool agent (sniffer, dummy, logger, introspector, df-gui) | [x] | |
| 9.21 | `/api/df/registrations/:agentName` | PUT | Modify a DF registration | [x] | |
| 9.22 | `/api/df/federation/:parentDFName` | DELETE | Deregister from a parent DF | [x] | |
| 9.23 | `/api/df/federation/children/:childDFName` | DELETE | Deregister a child DF | [x] | |

**Note:** The old GUI communicates with the backend via ACL messages (FIPA protocols), not REST. The new React GUI must use REST. Many functionalities above require **new REST endpoints** to be added to `RestAPIVerticle.java`. Each migration task should include:
1. Add/extend the REST endpoint in RestAPIVerticle
2. Implement the action in the rma/agent backend class to serve the REST call
3. Implement the React component
4. Test end-to-end

---

## Testing Checklist (Definition of Done per Feature)

For every functionality migrated, complete these steps:

### Backend (REST API)
- [ ] REST endpoint returns correct HTTP status codes (200, 400, 403, 404, 500)
- [ ] REST endpoint returns correct JSON schema
- [ ] REST endpoint correctly calls the underlying JADE management API (AMS/JADEAgentManagement ontology)
- [ ] Error handling returns meaningful error messages
- [ ] REST endpoint is listed and documented in Boot.java / AGENTS.md

### Frontend (React GUI)
- [ ] React page/component renders correctly
- [ ] Component calls correct REST endpoint(s)
- [ ] Component handles success/error states gracefully
- [ ] User interactions trigger correct backend actions
- [ ] Page is responsive and follows existing design patterns

### Integration Testing
- [ ] End-to-end test: UI action → REST call → backend action → platform state change observed
- [ ] Verify against running JADE platform (Docker compose or local)
- [ ] Test error scenarios (e.g., kill non-existent agent)
- [ ] Verify results reflected in UI when polling/subscribing to updates

### Documentation
- [ ] Feature tested and documented in this tracker
- [ ] Any new REST endpoints added to the API reference list above

---

## Migration Priority Recommendations

The following order is suggested for implementation (highest impact first):

1. **Platform overview** — `/api/platform` → dashboard with platform ID, container name, AMS, DF, main container status
2. **Agent list** — `/api/agents` → extend to show agent states, containers, ownership
3. **Agent lifecycle** — Start/Kill/Suspend/Resume agents (via AMS ACL messages from backend)
4. **Agent migration/cloning** — Move/Clone agents across containers
5. **DF GUI (Directory Facilitator)** — Register/Deregister/Search agents in the DF
6. **Sniffer** — Intercept and display ACL messages
7. **Introspector** — Debug agent behaviours (most complex)
8. **Log Manager** — Container log management
9. **Dummy Agent / Test Agent** — Manual message composition and sending

---

## API Specification Files

The following files contain detailed input/output specifications for each tool's REST API endpoints, to be used as the basis for backend development:

| Tool | API Spec File |
|------|--------------|
| **RMA (Remote Management Agent)** | [docs/api/rma-api.md](api/rma-api.md) |
| **Sniffer** | [docs/api/sniffer-api.md](api/sniffer-api.md) |
| **DF GUI (Directory Facilitator)** | [docs/api/df-gui-api.md](api/df-gui-api.md) |
| **Introspector (Agent Debugger)** | [docs/api/introspector-api.md](api/introspector-api.md) |
| **Log Manager Agent** | [docs/api/log-manager-api.md](api/log-manager-api.md) |
| **Dummy Agent** | [docs/api/dummy-agent-api.md](api/dummy-agent-api.md) |
| **Test Agent** | [docs/api/test-agent-api.md](api/test-agent-api.md) |
| **Shared GUI Components** | [docs/api/shared-components-api.md](api/shared-components-api.md) |
