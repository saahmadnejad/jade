package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for the health-check endpoint ({@code GET /api/health}).
 * Returns a static JSON response indicating the REST server is alive.
 *
 * <p><b>Old GUI implementation:</b> No direct Swing GUI equivalent existed for
 * this endpoint — it is a new REST-only health check. The old RMA tool
 * ({@code io.donbee.jade.tools.rma.rma}) did not expose a liveness probe;
 * the closest analogue is the RMA's connection to the platform, established
 * via {@code rma.setup()} and the {@code AMSSubscribe} behaviour
 * ({@code io.donbee.jade.tools.rma.rma:164}).</p>
 */
public class HealthHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext ctx) {
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("status", "ok").toBuffer());
    }
}
