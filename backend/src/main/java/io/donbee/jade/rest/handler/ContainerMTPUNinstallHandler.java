package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for DELETE /api/containers/:name/mtps/:address — uninstall an MTP from a container.
 */
public class ContainerMTPUNinstallHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public ContainerMTPUNinstallHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String containerName = ctx.pathParams().get("name");
        String address = ctx.pathParams().get("address");
        if (containerName == null || containerName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing container name"));
            return;
        }
        if (address == null || address.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing address"));
            return;
        }
        try {
            service.uninstallMTP(containerName, address);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "MTP at '" + address + "' uninstalled from '" + containerName + "'")
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, new RuntimeException("MTP uninstallation failed: " + e.getMessage()));
        }
    }
}
