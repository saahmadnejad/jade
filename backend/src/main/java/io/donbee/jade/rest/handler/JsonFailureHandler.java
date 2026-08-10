package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Failure handler that converts Vert.x routing failures into
 * structured JSON error responses: {"error": "message", "code": N}
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
