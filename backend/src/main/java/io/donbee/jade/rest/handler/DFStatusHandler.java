package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFStatus;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for GET /api/tools/df-gui/status — returns DF runtime status.
 */
public class DFStatusHandler implements Handler<RoutingContext> {

    private final DFService service;

    public DFStatusHandler(DFService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            DFStatus status = service.getDFStatus();
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("running", status.running)
                    .put("agent", status.agent)
                    .put("container", status.container)
                    .put("registeredAgentCount", status.registeredAgentCount)
                    .put("parentCount", status.parentCount)
                    .put("childCount", status.childCount)
                    .toBuffer());
        } catch (IllegalStateException e) {
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("running", false).toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
