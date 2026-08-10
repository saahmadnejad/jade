package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * Handler for GET /api/platforms — list remote platforms.
 */
public class RemotePlatformListHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public RemotePlatformListHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        try {
            List<PlatformService.RemotePlatformInfo> platforms = service.getRemotePlatforms();
            JsonArray arr = new JsonArray();
            for (PlatformService.RemotePlatformInfo p : platforms) {
                arr.add(new JsonObject()
                    .put("name", p.name)
                    .put("ams", p.ams)
                    .put("addresses", new JsonArray(java.util.Arrays.asList(p.addresses)))
                    .put("services", new JsonArray(java.util.Arrays.asList(p.services))));
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("platforms", arr).toBuffer());
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
