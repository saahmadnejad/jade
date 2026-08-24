package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Failure handler that converts Vert.x routing failures into
 * structured JSON error responses: {@code {"error": "message", "code": N}}.
 *
 * <p><b>Old GUI implementation:</b> The old Swing GUI tools (RMA, DF GUI,
 * Sniffer, etc.) displayed errors via Swing dialogs:
 * {@code myGUI.showErrorDialog()}
 * ({@code io.donbee.jade.tools.rma.rma:87}) showed a
 * {@code JOptionPane.showMessageDialog} for {@code NOT-UNDERSTOOD},
 * {@code REFUSE}, {@code FAILURE}, etc. This handler replaces that
 * ad-hoc dialog pattern with a consistent HTTP error format suitable
 * for the REST API.</p>
 */
public class JsonFailureHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        int statusCode = ctx.statusCode();
        if (statusCode < 400) {
            statusCode = 500;
        }
        String message = ctx.failure() != null
            ? ctx.failure().getMessage()
            : "Unexpected error";
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("error", message).put("code", statusCode).encode());
    }
}
