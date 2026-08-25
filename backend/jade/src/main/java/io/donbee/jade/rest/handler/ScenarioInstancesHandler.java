package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.service.ScenarioService;

/**
 * Handler for {@code GET /api/scenarios/instances} — lists running scenario
 * instances tracked since platform start.
 *
 * <p><b>Old GUI implementation:</b>
 * No direct Swing GUI equivalent; corresponds to reading the RMA agent tree
 * for manually started demo agents
 * ({@code io.donbee.jade.gui.AgentTree}). Delegates to
 * {@link ScenarioService#listInstances()}.</p>
 */
public class ScenarioInstancesHandler implements Handler<RoutingContext> {

    private final ScenarioService scenarioService;

    public ScenarioInstancesHandler(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            JsonArray arr = new JsonArray();
            for (ScenarioService.InstanceInfo info : scenarioService.listInstances()) {
                JsonArray agents = new JsonArray();
                info.agents().forEach(name -> agents.add(new JsonObject().put("name", name)));
                arr.add(new JsonObject()
                    .put("instance", info.instance())
                    .put("scenarioId", info.scenarioId())
                    .put("container", info.container())
                    .put("agents", agents));
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("instances", arr).toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
