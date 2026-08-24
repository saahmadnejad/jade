package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;
import io.donbee.jade.core.VersionManager;

/**
 * Handler for the version endpoint ({@code GET /api/version}).
 * Returns JADE version, revision, and date extracted from
 * {@link io.donbee.jade.core.VersionManager}.
 *
 * <p><b>Old GUI implementation:</b> The old RMA tool displayed version
 * information through {@code io.donbee.jade.gui.AboutJadeAction} and
 * {@code io.donbee.jade.Version}. The underlying version data is read
 * from the same {@code VersionManager} (a.k.a. {@code Version}}
 * singleton), which reads the {@code version.properties} file bundled
 * in the JADE JAR.</p>
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
