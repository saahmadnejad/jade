package io.donbee.jade.rest.handler;

import io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;
import io.donbee.jade.domain.FIPAAgentManagement.SearchConstraints;
import io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription;
import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFRegistrationInfo;
import io.donbee.jade.rest.service.DFService.DFServiceInfo;
import io.donbee.jade.rest.service.DFService.SearchConstraintsInfo;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for DF search: {@code POST /api/df/search}.
 * Searches for agents registered with the DF matching the given
 * template and constraints.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.dfgui.DFGUISearchAction}
 * ({@code actionPerformed}) used a {@code ConstraintDlg} to collect
 * search constraints (max depth, max results) and a
 * {@code DFAgentDscDlg} to collect the search template, then posted
 * a {@code DFGUIAdapter.SEARCH} GuiEvent to the DF agent. The DF agent
 * performed the search via {@code io.donbee.jade.domain.DFService#search()}
 * (the static helper in the domain package, not this REST service)
 * and returned results to the GUI. This REST handler delegates to
 * {@code DFService#searchDF()} which performs the same search but
 * through the synchronous {@code DFRequestAgent} helper.</p>
 */
public class DFSearchHandler implements Handler<RoutingContext> {

    private final DFService service;

    public DFSearchHandler(DFService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                ctx.fail(400, new RuntimeException("Request body is required"));
                return;
            }

            DFAgentDescription template = new DFAgentDescription();

            JsonObject desc = body.getJsonObject("description");
            if (desc != null) {
                String agentName = desc.getString("name");
                if (agentName != null && !agentName.isEmpty()) {
                    io.donbee.jade.core.AID aid = new io.donbee.jade.core.AID();
                    aid.setName(agentName);
                    template.setName(aid);
                }
                if (desc.getJsonArray("services") != null) {
                    desc.getJsonArray("services").forEach(item -> {
                        JsonObject svc = (JsonObject) item;
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType(svc.getString("type"));
                        if (svc.getString("name") != null) {
                            sd.setName(svc.getString("name"));
                        }
                        template.addServices(sd);
                    });
                }
            }

            SearchConstraintsInfo constraints = new SearchConstraintsInfo(null, -1L);
            JsonObject constraintsObj = body.getJsonObject("constraints");
            if (constraintsObj != null) {
                Long maxDepth = constraintsObj.getLong("maxDepth");
                Long maxResults = constraintsObj.getLong("maxResults");
                constraints = new SearchConstraintsInfo(maxDepth, maxResults);
            }

            List<DFRegistrationInfo> results = service.searchDF(template, constraints);

            List<JsonObject> resultList = new ArrayList<>();
            for (DFRegistrationInfo reg : results) {
                resultList.add(toJson(reg));
            }

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("results", resultList)
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }

    private JsonObject toJson(DFRegistrationInfo reg) {
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
}
