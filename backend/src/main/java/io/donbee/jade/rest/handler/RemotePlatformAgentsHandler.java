package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * Handler for GET /api/platforms/:name/agents — list agents on a remote platform.
 */
public class RemotePlatformAgentsHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public RemotePlatformAgentsHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String platformName = ctx.pathParams().get("name");
        if (platformName == null || platformName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing platform name"));
            return;
        }
        try {
            List<PlatformService.AgentInfo> agents = service.searchRemotePlatformAgents(platformName);
            JsonArray arr = new JsonArray();
            for (PlatformService.AgentInfo a : agents) {
                arr.add(new JsonObject()
                    .put("name", a.name)
                    .put("addresses", new JsonArray(java.util.Arrays.asList(a.addresses))));
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("agents", arr).toBuffer());
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
