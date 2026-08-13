package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFParentInfo;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for DF federation endpoints:
 * <ul>
 *   <li>{@code GET  /api/df/federation/parents}               — List parent DFs</li>
 *   <li>{@code GET  /api/df/federation/children}              — List child DFs</li>
 *   <li>{@code POST /api/df/federation}                       — Federate with a parent DF</li>
 *   <li>{@code DELETE /api/df/federation/{parentDFName}}      — Deregister from a parent DF</li>
 *   <li>{@code DELETE /api/df/federation/children/{childDFName}} — Deregister a child DF</li>
 * </ul>
 *
 * <p><b>Old GUI implementation:</b> The federation tables were part of
 * {@code io.donbee.jade.tools.dfgui.DFGUI} (the "DF Federation" tab),
 * populated by {@code DFGUI#refresh()}
 * ({@code io.donbee.jade.tools.dfgui.DFGUI:786}).
 * <ul>
 *   <li><b>Parents:</b> Populated from
 *       {@code DFGUIAdapter#getParents()} via a
 *       {@code GetParents} action ({@code DFGUIManagement} ontology).
 *       New parents were added via
 *       {@code io.donbee.jade.tools.dfgui.DFGUIFederateAction},
 *       which collected the parent AID via {@code AIDGui} and the
 *       description via {@code DFAgentDscDlg}, then posted a
 *       {@code DFGUIAdapter.FEDERATE} GuiEvent.</li>
 *   <li><b>Children:</b> Children were DFs (registered with this DF
 *       that expose the {@code "fipa-df"} service type) — identified
 *       during the {@code refresh()} search, not via a dedicated
 *       API. The old GUI filtered registrations by service type.</li>
 *   <li><b>Deregister parent:</b> Done via
 *       {@code io.donbee.jade.tools.dfgui.DFGUIDeregisterAction}
 *       when {@code kind == DFGUI.PARENT_VIEW}, which posted a
 *       {@code DeregisterFrom} action to the selected parent DF.</li>
 *   <li><b>Deregister child:</b> Same deregister action when
 *       {@code kind == DFGUI.CHILDREN_VIEW} — a standard
 *       {@code Deregister} from this DF.</li>
 * </ul>
 * This REST handler delegates to {@code DFService} methods
 * ({@code getDFParents()}, {@code getDFChildren()},
 * {@code federateDF()}, {@code deregisterParentDF()},
 * {@code deregisterChildDF()}) which replicate the same FIPA
 * {@code DFGUIManagement} ontology operations.</p>
 */
public class DFederationHandler implements Handler<RoutingContext> {

    public enum Mode { PARENTS, CHILDREN, FEDERATE, DEREGISTER_PARENT, DEREGISTER_CHILD }

    private final DFService service;
    private final Mode mode;

    public DFederationHandler(DFService service, Mode mode) {
        this.service = service;
        this.mode = mode;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String dfName = ctx.pathParams() != null ? ctx.pathParams().get(pathParamName()) : null;

        try {
            switch (mode) {
                case PARENTS:
                    handleParents(ctx);
                    break;
                case CHILDREN:
                    handleChildren(ctx);
                    break;
                case FEDERATE:
                    handleFederate(ctx);
                    break;
                case DEREGISTER_PARENT:
                    handleDeregisterParent(ctx, dfName);
                    break;
                case DEREGISTER_CHILD:
                    handleDeregisterChild(ctx, dfName);
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

    private String pathParamName() {
        return mode == Mode.DEREGISTER_PARENT ? "parentDFName" : "childDFName";
    }

    private void handleParents(RoutingContext ctx) {
        List<DFParentInfo> parents = service.getDFParents();
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(toJson("parents", parents).toBuffer());
    }

    private void handleChildren(RoutingContext ctx) {
        List<DFParentInfo> children = service.getDFChildren();
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(toJson("children", children).toBuffer());
    }

    private void handleFederate(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.fail(400, new RuntimeException("Request body is required"));
            return;
        }
        String parentDFName = body.getString("parentDF");
        if (parentDFName == null || parentDFName.isEmpty()) {
            ctx.fail(400, new RuntimeException("parentDF is required"));
            return;
        }

        List<String> addresses = new ArrayList<>();
        if (body.getJsonArray("parentDFAddresses") != null) {
            body.getJsonArray("parentDFAddresses").forEach(a -> addresses.add((String) a));
        }

        DFParentInfo parent = service.federateDF(parentDFName, addresses);
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("message", "DF federated with " + parentDFName)
                .put("parent", parentToJson(parent))
                .toBuffer());
    }

    private void handleDeregisterParent(RoutingContext ctx, String parentDFName) {
        if (parentDFName == null || parentDFName.isEmpty()) {
            ctx.fail(400, new RuntimeException("parentDFName is required"));
            return;
        }
        service.deregisterParentDF(parentDFName);
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("message", "Deregistered from parent DF '" + parentDFName + "'")
                .toBuffer());
    }

    private void handleDeregisterChild(RoutingContext ctx, String childDFName) {
        if (childDFName == null || childDFName.isEmpty()) {
            ctx.fail(400, new RuntimeException("childDFName is required"));
            return;
        }
        service.deregisterChildDF(childDFName);
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("message", "Child DF '" + childDFName + "' deregistered")
                .toBuffer());
    }

    // ---- JSON serialization helpers ----

    private JsonObject toJson(String key, List<DFParentInfo> items) {
        JsonArray arr = new JsonArray();
        if (items != null) {
            for (DFParentInfo p : items) {
                arr.add(parentToJson(p));
            }
        }
        return new JsonObject().put(key, arr);
    }

    private JsonObject parentToJson(DFParentInfo p) {
        JsonArray addrs = new JsonArray();
        if (p.addresses != null) {
            for (String a : p.addresses) {
                addrs.add(a);
            }
        }
        return new JsonObject()
            .put("name", p.name != null ? p.name : "")
            .put("addresses", addrs);
    }
}
