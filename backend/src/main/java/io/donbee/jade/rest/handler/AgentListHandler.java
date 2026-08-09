package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for the agent-list endpoint.
 */
public class AgentListHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentListHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        try {
            boolean detail = "true".equalsIgnoreCase(ctx.queryParams().get("detail"));
            java.util.List<PlatformService.AgentInfo> agents = service.getAgents(detail);
            JsonArray arr = new JsonArray();
            for (PlatformService.AgentInfo ai : agents) {
                JsonObject obj = new JsonObject().put("name", ai.name);
                if (detail) {
                    obj.put("state", ai.state != null ? ai.state : "UNKNOWN")
                       .put("ownership", ai.ownership != null ? ai.ownership : "")
                       .put("container", ai.container)
                       .put("addresses", jsonFromArray(ai.addresses));
                }
                arr.add(obj);
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("agents", arr).toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }

    private JsonArray jsonFromArray(String[] arr) {
        JsonArray j = new JsonArray();
        for (String s : arr) j.add(s);
        return j;
    }
}
