package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for retrieving a single container by name.
 */
public class ContainerInfoHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public ContainerInfoHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String containerName = ctx.pathParams().get("name");
        if (containerName == null || containerName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing container name"));
            return;
        }
        try {
            PlatformService.ContainerInfo container = service.getContainer(containerName);
            if (container == null) {
                ctx.fail(404, new IllegalArgumentException("Container not found: " + containerName));
                return;
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("name", container.name)
                    .put("address", container.address)
                    .put("port", container.port)
                    .put("isMain", container.isMain)
                    .toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
