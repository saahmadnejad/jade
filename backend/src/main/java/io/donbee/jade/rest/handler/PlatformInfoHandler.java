package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for the platform-info endpoint ({@code GET /api/platform}).
 * Returns platform ID, container name, main-container flag, AMS name,
 * and default DF name.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.MainWindow} rendered the platform
 * root folder in the {@code AgentTree} and displayed platform
 * properties. The {@code viewAPDescription()} methods in
 * {@code io.donbee.jade.tools.rma.rma} (lines 1064–1070) showed the
 * {@code APDescription} via a Swing dialog. The data itself was
 * obtained from {@code impl.getPlatformID()}, {@code impl.here()},
 * {@code impl.getAMS()}, and {@code impl.getDefaultDF()} — the same
 * calls used in {@code JadesPlatformService#getPlatformInfo()}
 * (see service layer).</p>
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
