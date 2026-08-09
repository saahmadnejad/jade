package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;
import io.donbee.jade.core.VersionManager;

/**
 * Handler for the version endpoint.
 */
public class VersionHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext ctx) {
        JsonObject resp = new JsonObject()
            .put("version", "UNKNOWN")
            .put("revision", "UNKNOWN")
            .put("date", "UNKNOWN");
        VersionManager vm = new VersionManager();
        try {
            resp.put("version", vm.getVersion());
            resp.put("revision", vm.getRevision());
            resp.put("date", vm.getDate());
        } catch (Exception ignored) {}
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(resp.toBuffer());
    }
}
