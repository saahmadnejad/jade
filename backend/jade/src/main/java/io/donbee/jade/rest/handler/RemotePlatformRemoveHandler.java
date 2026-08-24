package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code DELETE /api/platforms/:name} — remove a remote
 * platform.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.RemoveRemoteAMSAction} invoked
 * {@code rma.removeRemotePlatform(apDesc)}
 * ({@code rma.java:1072}), which called
 * {@code myGUI.removeRemotePlatform(name)} to remove the platform
 * from the {@code MainWindow} tree. The old implementation was a
 * local GUI state change only (no AMS messaging). This handler
 * delegates to {@code JadesPlatformService#removeRemotePlatform()}.</p>
 */
public class RemotePlatformRemoveHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public RemotePlatformRemoveHandler(PlatformService service) {
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
            service.removeRemotePlatform(platformName);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Remote platform removed").toBuffer());
        } catch (UnsupportedOperationException e) {
            ctx.fail(501, e);
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
