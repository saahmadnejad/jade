package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for the platform-shutdown endpoint ({@code POST /api/platform/shutdown}).
 * Sends a {@code ShutdownPlatform} action to the AMS, terminating the entire
 * JADE platform. Only the main container may call this.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.ShutDownAction} (line 41–43) called
 * {@code rma.shutDownPlatform()} which, in turn, built a
 * {@link io.donbee.jade.domain.JADEAgentManagement.ShutdownPlatform}
 * action and sent it to the AMS via an
 * {@code AMSClientBehaviour}
 * ({@code io.donbee.jade.tools.rma.rma:906}). The old implementation
 * also showed a confirmation dialog ({@code showExitDialog}); this
 * handler relies on the caller (REST API consumer) to confirm.</p>
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
