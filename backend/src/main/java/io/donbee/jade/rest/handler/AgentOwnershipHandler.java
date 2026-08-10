package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for PATCH /api/agents/:name (change ownership).
 */
public class AgentOwnershipHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentOwnershipHandler(PlatformService service) {
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
        String newOwner = null;
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                newOwner = json.getString("ownership");
                if (newOwner == null) {
                    newOwner = json.getString("newOwner");
                }
            }
        }
        if (newOwner == null || newOwner.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing ownership"));
            return;
        }
        try {
            service.changeAgentOwnership(agentName, newOwner);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Agent '" + agentName + "' ownership changed to '" + newOwner + "'")
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
