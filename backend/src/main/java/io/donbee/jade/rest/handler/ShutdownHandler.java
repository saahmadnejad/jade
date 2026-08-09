package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for the platform-shutdown endpoint.
 */
public class ShutdownHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public ShutdownHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        try {
            service.shutdownPlatform();
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Platform shutdown initiated").toBuffer());
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
