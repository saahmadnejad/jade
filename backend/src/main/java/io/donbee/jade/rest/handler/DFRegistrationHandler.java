package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFRegistrationInfo;
import io.donbee.jade.rest.service.DFService.DFServiceInfo;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for DF registration management endpoints:
 * <ul>
 *   <li>{@code GET /api/df/registrations}         — List all registrations</li>
 *   <li>{@code POST /api/df/registrations}        — Register an agent</li>
 *   <li>{@code GET /api/df/registrations/{name}}  — View a specific registration</li>
 *   <li>{@code PUT /api/df/registrations/{name}}  — Modify a registration</li>
 *   <li>{@code DELETE /api/df/registrations/{name}} — Deregister an agent</li>
 * </ul>
 *
 * <p><b>Old GUI implementation:</b> The old DF GUI was
 * {@code io.donbee.jade.tools.dfgui.DFGUI}, a Swing application with
 * three tabs (Registrations, Search Result, DF Federation). The
 * registration table ({@code DFGUI.registeredTable}) was populated by
 * {@code DFGUI#refresh()}, which called
 * {@code DFGUIAdapter#getAllAgentsRegistered()} (via
 * {@code DFGUIAdapter}.
 * <ul>
 *   <li><b>List:</b> {@code DFGUI.java:786} {@code refresh()} method.</li>
 *   <li><b>Register:</b> {@code io.donbee.jade.tools.dfgui.DFGUIRegisterAction}
 *       ({@code actionPerformed}), which used a {@code DFAgentDscDlg}
 *       to collect the agent description and posted a
 *       {@code DFGUIAdapter.REGISTER} GuiEvent.</li>
 *   <li><b>View:</b> {@code io.donbee.jade.tools.dfgui.DFGUIViewAction},
 *       which retrieved the description via
 *       {@code DFGUIAdapter#getDFAgentDsc(AID)} and displayed it in a
 *       {@code DFAgentDscDlg}.</li>
 *   <li><b>Modify:</b> {@code io.donbee.jade.tools.dfgui.DFGUIModifyAction},
 *       which retrieved the old description, showed it in a
 *       {@code DFAgentDscDlg}, then posted a
 *       {@code DFGUIAdapter.MODIFY} GuiEvent.</li>
 *   <li><b>Deregister:</b> {@code io.donbee.jade.tools.dfgui.DFGUIDeregisterAction},
 *       which posted a {@code DFGUIAdapter.DEREGISTER} GuiEvent.</li>
 * </ul>
 * The old GUI used {@code DFGUIAdapter} (a Swing-specific adapter
 * bridging to the DF agent) to translate GuiEvents into FIPA
 * {@code Register}, {@code Deregister}, {@code Modify}, and
 * {@code Search} ACL messages. This REST handler bypasses the GUI
 * layer entirely, performing the same FIPA operations directly
 * through {@code DFRequestAgent} (see {@code DFService} interface).</p>
 */
public class DFRegistrationHandler implements Handler<RoutingContext> {

    public enum Mode { LIST, REGISTER, VIEW, MODIFY, DEREGISTER }

    private final DFService service;
    private final Mode mode;

    public DFRegistrationHandler(DFService service, Mode mode) {
        this.service = service;
        this.mode = mode;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String agentName = ctx.pathParams() != null ? ctx.pathParams().get("agentName") : null;

        try {
            switch (mode) {
                case LIST:
                    handleList(ctx);
                    break;
                case REGISTER:
                    handleRegister(ctx);
                    break;
                case VIEW:
                    handleView(ctx, agentName);
                    break;
                case MODIFY:
                    handleModify(ctx, agentName);
                    break;
                case DEREGISTER:
                    handleDeregister(ctx, agentName);
                    break;
            }
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }

    private void handleList(RoutingContext ctx) {
        List<DFRegistrationInfo> regs = service.listDFRegistrations();
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(toJson(regs).toBuffer());
    }

    private void handleRegister(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.fail(400, new RuntimeException("Request body is required"));
            return;
        }
        String agentName = body.getString("agentName");
        if (agentName == null || agentName.isEmpty()) {
            ctx.fail(400, new RuntimeException("agentName is required"));
            return;
        }

        List<String> addresses = new ArrayList<>();
        if (body.getJsonArray("addresses") != null) {
            body.getJsonArray("addresses").forEach(a -> addresses.add((String) a));
        }

        List<DFServiceInfo> services = parseServices(body.getJsonArray("services"));

        DFRegistrationInfo result = service.registerWithDF(agentName, addresses, services);
        ctx.response()
            .setStatusCode(201)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("message", "Agent '" + agentName + "' registered with DF")
                .put("registration", registrationToJson(result))
                .toBuffer());
    }

    private void handleView(RoutingContext ctx, String agentName) {
        if (agentName == null || agentName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Agent name is required"));
            return;
        }
        DFRegistrationInfo info = service.getDFRegistration(agentName);
        if (info == null) {
            ctx.fail(404, new RuntimeException("Agent not found in DF: " + agentName));
            return;
        }
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(registrationToJson(info).toBuffer());
    }

    private void handleModify(RoutingContext ctx, String agentName) {
        if (agentName == null || agentName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Agent name is required"));
            return;
        }
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.fail(400, new RuntimeException("Request body is required"));
            return;
        }

        List<String> addresses = new ArrayList<>();
        if (body.getJsonArray("addresses") != null) {
            body.getJsonArray("addresses").forEach(a -> addresses.add((String) a));
        }

        List<DFServiceInfo> services = parseServices(body.getJsonArray("services"));

        DFRegistrationInfo result = service.modifyDFRegistration(agentName, addresses, services);
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("message", "Registration modified")
                .put("registration", registrationToJson(result))
                .toBuffer());
    }

    private void handleDeregister(RoutingContext ctx, String agentName) {
        if (agentName == null || agentName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Agent name is required"));
            return;
        }
        service.deregisterFromDF(agentName);
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("message", "Agent '" + agentName + "' deregistered from DF")
                .toBuffer());
    }

    // ---- JSON serialization helpers ----

    private JsonObject toJson(List<DFRegistrationInfo> regs) {
        List<JsonObject> list = new ArrayList<>();
        for (DFRegistrationInfo reg : regs) {
            list.add(registrationToJson(reg));
        }
        return new JsonObject().put("registrations", list);
    }

    private JsonObject registrationToJson(DFRegistrationInfo reg) {
        List<JsonObject> services = new ArrayList<>();
        if (reg.services != null) {
            for (DFServiceInfo svc : reg.services) {
                services.add(new JsonObject()
                    .put("type", svc.type != null ? svc.type : "")
                    .put("name", svc.name != null ? svc.name : "")
                    .put("ownership", svc.ownership != null ? svc.ownership : ""));
            }
        }
        return new JsonObject()
            .put("name", reg.name != null ? reg.name : "")
            .put("addresses", new ArrayList<>(reg.addresses != null ? reg.addresses : java.util.Collections.emptyList()))
            .put("services", services)
            .put("ownership", reg.ownership != null ? reg.ownership : "");
    }

    private List<DFServiceInfo> parseServices(io.vertx.core.json.JsonArray arr) {
        List<DFServiceInfo> services = new ArrayList<>();
        if (arr != null) {
            arr.forEach(item -> {
                JsonObject svc = (JsonObject) item;
                services.add(new DFServiceInfo(
                    svc.getString("type"),
                    svc.getString("name"),
                    svc.getString("ownership")
                ));
            });
        }
        return services;
    }
}
