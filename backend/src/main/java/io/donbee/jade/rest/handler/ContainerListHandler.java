package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for the container-list endpoint.
 */
public class ContainerListHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public ContainerListHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        try {
            java.util.List<PlatformService.ContainerInfo> containers = service.getContainers();
            JsonArray arr = new JsonArray();
            for (PlatformService.ContainerInfo ci : containers) {
                arr.add(new JsonObject()
                    .put("name", ci.name)
                    .put("address", ci.address)
                    .put("port", ci.port)
                    .put("isMain", ci.isMain));
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("containers", arr).toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
