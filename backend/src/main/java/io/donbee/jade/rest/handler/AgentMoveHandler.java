package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

/**
 * Handler for POST /api/agents/:name/move and POST /api/agents/clone.
 */
public class AgentMoveHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentMoveHandler(PlatformService service) {
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
        RequestBody body = ctx.body();
        String targetContainer = null;
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                targetContainer = json.getString("container");
            }
        }
        if (targetContainer == null || targetContainer.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing container"));
            return;
        }
        try {
            service.moveAgent(agentName, targetContainer);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Agent '" + agentName + "' moved to '" + targetContainer + "'")
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
