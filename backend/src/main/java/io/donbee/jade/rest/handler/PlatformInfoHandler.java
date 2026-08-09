package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for the platform-info endpoint.
 */
public class PlatformInfoHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public PlatformInfoHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            PlatformService.PlatformInfo pi = service.getPlatformInfo();
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("platformID", pi.platformID)
                    .put("containerName", pi.containerName)
                    .put("isMain", pi.isMain)
                    .put("ams", pi.ams)
                    .put("defaultDF", pi.defaultDF)
                    .toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
