# REST API Documentation

**Purpose:** Quick reference for implemented REST endpoints. For detailed specifications including JADE ontology backend calls and full schemas, see individual API spec files:
- Core platform/agents/containers → [rma-api.md](rma-api.md)
- Shared components → [shared-components-api.md](shared-components-api.md)

## Old GUI Implementation References

Every REST handler class in `io.donbee.jade.rest.handler` includes a
**`<b>Old GUI implementation</b>`** JavaDoc section that names the
old Swing class, the old method/callback, the FIPA protocol/ontology
used, and the service-layer method the handler delegates to. This
allows developers to compare the old and new implementations side by side.

### Handler-to-Old-GUI Mapping

| REST Handler | Old Swing GUI Class(es) | RMA/DFGUI Method |
|---|---|---|
| `HealthHandler` | (new — no Swing equivalent) | RMA connection check (`rma.setup`) |
| `VersionHandler` | `io.donbee.jade.gui.AboutJadeAction` | `VersionManager` / `io.donbee.jade.Version` |
| `PlatformInfoHandler` | `io.donbee.jade.tools.rma.MainWindow` | `rma.viewAPDescription()` |
| `ShutdownHandler` | `io.donbee.jade.tools.rma.ShutDownAction` | `rma.shutDownPlatform()` |
| `ContainerListHandler` | `io.donbee.jade.gui.AgentTreeModel` | `rma.AMSListenerBehaviour` (AddedContainer) |
| `ContainerInfoHandler` | `io.donbee.jade.gui.AgentTree.ContainerNode` | `rma.AMSListenerBehaviour` |
| `ContainerKillHandler` | `io.donbee.jade.tools.rma.KillAction` | `rma.killContainer()` |
| `ContainerSaveHandler` | `io.donbee.jade.tools.rma.SaveContainerAction` | `rma.saveContainer()` |
| `ContainerLoadHandler` | `io.donbee.jade.tools.rma.LoadContainerAction` | `rma.loadContainer()` |
| `ContainerMTPInstallHandler` | `io.donbee.jade.tools.rma.InstallMTPAction` | `rma.installMTP()` |
| `ContainerMTPListHandler` | `io.donbee.jade.tools.rma.ManageMTPsAction` | `ManageMTPsDialog` |
| `ContainerMTPUNinstallHandler` | `io.donbee.jade.tools.rma.UninstallMTPAction` | `rma.uninstallMTP()` |
| `AgentListHandler` | `io.donbee.jade.gui.AgentTreeModel` | `rma.AMSListenerBehaviour` (BornAgent) |
| `AgentInfoHandler` | `io.donbee.jade.gui.AgentTree.AgentNode` | `rma.AMSListenerBehaviour` |
| `AgentDeployHandler` | `io.donbee.jade.tools.rma.StartNewAgentAction` | `rma.newAgent()` |
| `AgentActionHandler` (kill) | `io.donbee.jade.tools.rma.KillAction` | `rma.killAgent()` |
| `AgentActionHandler` (suspend) | `io.donbee.jade.tools.rma.SuspendAction` | `rma.suspendAgent()` |
| `AgentActionHandler` (resume) | `io.donbee.jade.tools.rma.ResumeAction` | `rma.resumeAgent()` |
| `AgentCloneHandler` | `io.donbee.jade.tools.rma.CloneAgentAction` | `rma.cloneAgent()` |
| `AgentMoveHandler` | `io.donbee.jade.tools.rma.MoveAgentAction` | `rma.moveAgent()` |
| `AgentSaveLoadHandler` (save) | `io.donbee.jade.tools.rma.SaveAgentAction` | `rma.saveAgent()` |
| `AgentSaveLoadHandler` (load) | `io.donbee.jade.tools.rma.LoadAgentAction` | `rma.loadAgent()` |
| `AgentFreezeThawHandler` (freeze) | `io.donbee.jade.tools.rma.FreezeAgentAction` | `rma.freezeAgent()` |
| `AgentFreezeThawHandler` (thaw) | `io.donbee.jade.tools.rma.ThawAgentAction` | `rma.thawAgent()` |
| `AgentOwnershipHandler` | `io.donbee.jade.tools.rma.ChangeAgentOwnershipAction` | `rma.changeAgentOwnership()` |
| `AgentRegisterRemoteHandler` | `io.donbee.jade.tools.rma.RegisterRemoteAgentAction` | `rma.registerRemoteAgentWithAMS()` |
| `ToolLaunchHandler` | `SnifferAction`, `DummyAgentAction`, `LogManagerAgentAction`, `IntrospectorAction`, `ShowDFGuiAction` | `rma.newAgent()` |
| `RemotePlatformListHandler` | `io.donbee.jade.tools.rma.MainWindow` | `rma.addRemotePlatform()` |
| `RemotePlatformAddHandler` | `io.donbee.jade.tools.rma.AddRemotePlatformAction` | `rma.addRemotePlatform()` |
| `RemotePlatformFetchHandler` | `io.donbee.jade.tools.rma.AddRemotePlatformFromURLAction` | `rma.addRemotePlatformFromURL()` |
| `RemotePlatformRemoveHandler` | `io.donbee.jade.tools.rma.RemoveRemoteAMSAction` | `rma.removeRemotePlatform()` |
| `RemotePlatformDescriptionHandler` | `io.donbee.jade.tools.rma.ViewAPDescriptionAction`, `RefreshAMSAgentAction` | `rma.viewAPDescription()` |
| `RemotePlatformAgentsHandler` | `io.donbee.jade.tools.rma.RefreshAMSAgentAction` | `rma.refreshRemoteAgent()` |
| `DFRegistrationHandler` | `io.donbee.jade.tools.dfgui.DFGUIRegisterAction`, `DFGUIViewAction`, `DFGUIModifyAction`, `DFGUIDeregisterAction` | `DFGUIAdapter` GuiEvents |
| `DFSearchHandler` | `io.donbee.jade.tools.dfgui.DFGUISearchAction` | `DFGUIAdapter.SEARCH` |
| `DFDescriptionHandler` | `io.donbee.jade.domain.DFGUIAdapter` | `getDescriptionOfThisDF()` |
| `DFRefreshHandler` | `io.donbee.jade.tools.dfgui.DFGUIRefreshAppletAction` | `DFGUI.refresh()` |
| `DFStatusHandler` | `io.donbee.jade.tools.dfgui.DFGUI` | `DFGUI.showStatusMsg()` |
| `DFederationHandler` (all modes) | `io.donbee.jade.tools.dfgui.DFGUIFederateAction`, `DFGUIDeregisterAction` | `DFGUIAdapter.FEDERATE` |
| `JsonFailureHandler` | `rma.showErrorDialog()` | `AMSClientBehaviour` error handlers |
| `MessagesRecentHandler` / `MessagesStreamHandler` | `io.donbee.jade.tools.sniffer.Sniffer` (message canvas), `tools.introspector.gui.MessagePanel` | see [messages-api.md](messages-api.md) |

## Endpoints Not Covered in Other Docs

These endpoints are implemented but not specified in other API docs:

### GET /api/health
Health check endpoint.

**Response: 200 OK**
```json
{"status": "ok"}
```

### GET /api/version
Returns JADE version information.

**Response: 200 OK**
```json
{"version": "string", "revision": "string", "date": "string"}
```

### GET /api/agents/:name
Returns details for a specific agent by local name.

**Path Parameters:**
- `name` — agent local name (e.g., `df`)

**Response: 200 OK**
```json
{
  "name": "df@host:1099/JADE",
  "state": "active",
  "ownership": "NONE",
  "container": "Main-Container",
  "addresses": ["http://host:7778/acc"]
}
```

### GET /api/containers/:name
Returns details for a specific container by name.

**Path Parameters:**
- `name` — container name (e.g., `Main-Container`)

**Response: 200 OK**
```json
{
  "name": "Main-Container",
  "address": "127.0.0.1",
  "port": "1099",
  "isMain": true
}
```

## Endpoints Covered in rma-api.md

The following endpoints are fully specified in [rma-api.md](rma-api.md):

| Method | Endpoint | Status |
|----------|----------|--------|
| GET | `/api/platform` | Specified & Implemented |
| POST | `/api/platform/shutdown` | Specified & Implemented |
| GET | `/api/containers` | Specified & Implemented |
| GET | `/api/containers/:name` | Specified & Implemented |
| DELETE | `/api/containers/:name` | Specified & Implemented |
| POST | `/api/containers/:name/save` | Specified & Implemented |
| POST | `/api/containers/:name/load` | Specified & Implemented |
| GET | `/api/containers/:name/mtps` | Specified & Implemented |
| POST | `/api/containers/:name/mtps` | Specified & Implemented |
| DELETE | `/api/containers/:name/mtps/:address` | Specified & Implemented |
| GET | `/api/agents` | Specified & Implemented |
| GET | `/api/agents/:name` | Specified & Implemented |
| POST | `/api/agents` | Specified & Implemented *(see implementation notes)* |
| DELETE | `/api/agents/:name` | Specified & Implemented |
| POST | `/api/agents/:name/suspend` | Specified & Implemented |
| POST | `/api/agents/:name/resume` | Specified & Implemented |
| POST | `/api/agents/:name/freeze` | Specified & Implemented |
| POST | `/api/agents/:name/thaw` | Specified & Implemented |
| POST | `/api/agents/clone` | Specified & Implemented |
| POST | `/api/agents/:name/move` | Specified & Implemented |
| POST | `/api/agents/:name/save` | Specified & Implemented |
| POST | `/api/agents/load` | Specified & Implemented |
| PATCH | `/api/agents/:name` | Specified & Implemented |
| POST | `/api/agents/register-remote` | Specified & Implemented |
| GET | `/api/platforms` | Specified & Implemented |
| POST | `/api/platforms` | Specified & Implemented |
| POST | `/api/platforms/fetch` | Specified & Implemented |
| DELETE | `/api/platforms/:name` | Specified & Implemented |
| GET | `/api/platforms/:name/description` | Specified & Implemented |
| POST | `/api/platforms/:name/refresh` | Specified & Implemented |
| GET | `/api/platforms/:name/agents` | Specified & Implemented |
| POST | `/api/tools/:tool/start` | Specified & Implemented |
| GET | `/api/messages/recent` | Specified & Implemented *(see [messages-api.md](messages-api.md))* |
| WS | `/api/messages/stream` | Specified & Implemented *(see [messages-api.md](messages-api.md))* |
| GET | `/api/scenarios` | Specified & Implemented *(see [scenarios-api.md](scenarios-api.md))* |
| POST | `/api/scenarios/:id/instances` | Specified & Implemented *(see [scenarios-api.md](scenarios-api.md))* |
| GET | `/api/scenarios/instances` | Specified & Implemented *(see [scenarios-api.md](scenarios-api.md))* |
| DELETE | `/api/scenarios/instances/:name` | Specified & Implemented *(see [scenarios-api.md](scenarios-api.md))* |

**Implementation note for POST /api/agents:** The implemented request body uses `class` (not `className`) and `args` (not `arguments`), both optional `container`/`owner` fields are omitted for simplicity.

## Error Format

All errors return JSON:
```json
{"error": "error message", "code": 404}
```

HTTP Status Codes:
- `200` — Success
- `201` — Created (agent deployed)
- `400` — Bad request (missing/invalid parameters)
- `403` — Forbidden (not a Main Container)
- `404` — Not found (agent/container doesn't exist)
- `409` — Conflict (agent name already exists)
- `500` — Internal server error
