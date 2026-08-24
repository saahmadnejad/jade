package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for retrieving a single agent by name
 * ({@code GET /api/agents/:name}).
 *
 * <p><b>Old GUI implementation:</b> Agent detail was displayed in
 * {@code io.donbee.jade.gui.AgentTree.AgentNode}, populated by
 * {@code rma.AMSListenerBehaviour} when an {@code BornAgent}
 * introspection event arrived ({@code rma.java:200}). The agent state
 * and ownership were obtained from
 * {@code AMSAgentDescription} via the AMS. This handler calls the same
 * {@code agentManager.getAMSDescription()} path through
 * {@code JadesPlatformService#getAgent()}.</p>
 */
public class AgentInfoHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentInfoHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String agentName = ctx.pathParams().get("name");
        if (agentName == null || agentName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing agent name"));
            return;
        }
        // Strip container suffix if present (e.g. "agent@container" -> "agent")
        if (agentName.contains("@")) {
            agentName = agentName.substring(0, agentName.indexOf('@'));
        }
        try {
            PlatformService.AgentInfo agent = service.getAgent(agentName);
            if (agent == null) {
                ctx.fail(404, new IllegalArgumentException("Agent not found: " + agentName));
                return;
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("name", agent.name)
                    .put("state", agent.state != null ? agent.state : "UNKNOWN")
                    .put("ownership", agent.ownership != null ? agent.ownership : "")
                    .put("container", agent.container)
                    .put("addresses", toArray(agent.addresses))
                    .toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }

    private io.vertx.core.json.JsonArray toArray(String[] arr) {
        io.vertx.core.json.JsonArray result = new io.vertx.core.json.JsonArray();
        if (arr != null) {
            for (String s : arr) {
                result.add(s);
            }
        }
        return result;
    }
}
