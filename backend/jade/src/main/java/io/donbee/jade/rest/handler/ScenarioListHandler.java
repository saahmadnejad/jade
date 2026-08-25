package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.scenario.Scenario;
import io.donbee.jade.rest.scenario.ScenarioParam;
import io.donbee.jade.rest.service.ScenarioService;

/**
 * Handler for {@code GET /api/scenarios} — lists the available scenario
 * templates with their configurable parameters and defaults.
 *
 * <p><b>Old GUI implementation:</b>
 * No direct Swing GUI equivalent: multi-agent demos were launched by
 * hand-written {@code Boot -agents} command lines (see
 * {@code io.donbee.jade.Boot} usage output). This handler reads the
 * scenarios discovered by {@link ScenarioService} via ServiceLoader.</p>
 */
public class ScenarioListHandler implements Handler<RoutingContext> {

    private final ScenarioService scenarioService;

    public ScenarioListHandler(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            JsonArray arr = new JsonArray();
            for (Scenario scenario : scenarioService.list()) {
                JsonObject params = new JsonObject();
                for (ScenarioParam p : scenario.params()) {
                    params.put(p.getName(), paramJson(p));
                }
                arr.add(new JsonObject()
                    .put("id", scenario.id())
                    .put("title", scenario.title())
                    .put("description", scenario.description())
                    .put("params", params));
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("scenarios", arr).toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }

    static JsonObject paramJson(ScenarioParam p) {
        JsonObject json = new JsonObject()
            .put("type", p.getType().name().toLowerCase())
            .put("defaultValue", p.getDefaultValue());
        if (p.getMinValue() != null) {
            json.put("minValue", p.getMinValue());
        }
        if (p.getMaxValue() != null) {
            json.put("maxValue", p.getMaxValue());
        }
        if (p.getDescription() != null) {
            json.put("description", p.getDescription());
        }
        return json;
    }
}
