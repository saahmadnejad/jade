package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Failure handler that converts Vert.x routing failures into
 * structured JSON error responses: {@code {"error": "message", "code": N, "exception": "...", "details": "..."}}.
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
        Throwable failure = ctx.failure();

        if (statusCode < 400) {
            if (failure instanceof IllegalArgumentException) {
                statusCode = 400;
            } else if (failure instanceof IllegalStateException) {
                statusCode = 403;
            } else {
                statusCode = 500;
            }
        }

        String message = extractMessage(failure);
        JsonObject errorBody = new JsonObject()
            .put("error", message)
            .put("code", statusCode);

        if (failure != null) {
            errorBody.put("exception", failure.getClass().getSimpleName());
            String details = extractDetails(failure);
            if (details != null && !details.isEmpty() && !details.equals(message)) {
                errorBody.put("details", details);
            }
        }

        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("Content-Type", "application/json")
            .end(errorBody.encode());
    }

    private String extractMessage(Throwable t) {
        if (t == null) {
            return "Unexpected error";
        }
        String msg = t.getMessage();
        Throwable current = t;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().trim().isEmpty()) {
                msg = current.getMessage();
            }
            current = current.getCause();
        }
        if (msg == null || msg.trim().isEmpty()) {
            return t.getClass().getSimpleName();
        }
        if (msg.contains("- Caused by:")) {
            int idx = msg.lastIndexOf("- Caused by:");
            String part = msg.substring(idx + "- Caused by:".length()).trim();
            if (!part.isEmpty()) {
                msg = part;
            }
        }
        return msg.trim();
    }

    private String extractDetails(Throwable t) {
        if (t == null || t.getCause() == null) {
            return null;
        }
        return t.getCause().getMessage() != null
            ? t.getCause().getMessage()
            : t.getCause().toString();
    }
}
