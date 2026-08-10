package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * Handler for GET /api/containers/:name/mtps — list MTPs on a container.
 */
public class ContainerMTPListHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public ContainerMTPListHandler(PlatformService service) {
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
            List<PlatformService.MTPInfo> mtps = service.getMTPs(containerName);
            io.vertx.core.json.JsonArray arr = new io.vertx.core.json.JsonArray();
            for (PlatformService.MTPInfo mtp : mtps) {
                arr.add(new JsonObject()
                    .put("address", mtp.address)
                    .put("className", mtp.className));
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("mtps", arr).toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, new RuntimeException("Failed to list MTPs: " + e.getMessage()));
        }
    }
}
