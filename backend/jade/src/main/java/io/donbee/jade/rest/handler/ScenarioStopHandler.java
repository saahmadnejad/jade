package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.service.ScenarioService;

/**
 * Handler for {@code DELETE /api/scenarios/instances/:name} — stops a running
 * scenario instance by killing its dedicated container.
 *
 * <p><b>Old GUI implementation:</b>
 * Killing a set of demo agents previously required killing each agent through
 * the RMA tree ({@code io.donbee.jade.tools.rma.KillAction} calling
 * {@code rma.killAgent(id)}, {@code rma.java:644}) one by one. This handler
 * delegates to {@link ScenarioService#stop(String)} which kills the instance's
 * whole container via {@code PlatformService#killContainer()}.</p>
 */
public class ScenarioStopHandler implements Handler<RoutingContext> {

    private final ScenarioService scenarioService;

    public ScenarioStopHandler(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String instance = ctx.pathParam("name");
        try {
            scenarioService.stop(instance);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "Instance '" + instance + "' stopped")
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (RuntimeException e) {
            ctx.fail(500, e);
        }
    }
}
