package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for POST /api/df/refresh — refreshes all DF data.
 */
public class DFRefreshHandler implements Handler<RoutingContext> {

    private final DFService service;

    public DFRefreshHandler(DFService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            var status = service.getDFStatus();
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "DF refreshed")
                    .put("registeredAgentCount", status.registeredAgentCount)
                    .put("parentCount", status.parentCount)
                    .put("childCount", status.childCount)
                    .toBuffer());
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
