package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for POST /api/agents/register-remote — register a remote agent with the local AMS.
 */
public class AgentRegisterRemoteHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentRegisterRemoteHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        RequestBody body = ctx.body();
        if (body == null) {
            ctx.fail(400, new RuntimeException("Request body is required"));
            return;
        }
        JsonObject json = body.asJsonObject();
        if (json == null) {
            ctx.fail(400, new RuntimeException("Request body is required"));
            return;
        }
        String aid = json.getString("aid");
        if (aid == null || aid.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing aid in request body"));
            return;
        }
        String[] addresses = null;
        if (json.getJsonArray("addresses") != null) {
            java.util.List<String> list = json.getJsonArray("addresses").getList();
            addresses = list.toArray(new String[0]);
        }
        try {
            service.registerRemoteAgent(aid, addresses);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Agent '" + aid + "' registered with local AMS")
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
