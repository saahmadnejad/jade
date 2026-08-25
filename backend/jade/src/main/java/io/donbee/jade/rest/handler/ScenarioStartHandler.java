package io.donbee.jade.rest.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.service.ScenarioService;

/**
 * Handler for {@code POST /api/scenarios/{id}/instances} — starts a new
 * instance of a scenario in its own dedicated container.
 *
 * <p><b>Old GUI implementation:</b>
 * No direct Swing GUI equivalent: launching demo agent sets required
 * hand-written {@code Boot -agents} specifiers or {@code -conf} property
 * files ({@code io.donbee.jade.Boot}). This handler delegates to
 * {@link ScenarioService#start(String, String, Map)} which in turn uses
 * {@code PlatformService#deployAgent()} per agent.</p>
 */
public class ScenarioStartHandler implements Handler<RoutingContext> {

    private final ScenarioService scenarioService;

    public ScenarioStartHandler(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String scenarioId = ctx.pathParam("id");
        try {
            JsonObject body = ctx.body().asJsonObject();
            String instanceName = body != null ? body.getString("instanceName") : null;
            Map<String, Object> config = readConfig(body);

            ScenarioService.StartResult result = scenarioService.start(scenarioId, instanceName, config);

            JsonArray agents = new JsonArray();
            result.agents.forEach(agents::add);
            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "Scenario '" + result.scenarioId + "' started as instance '" + result.instance + "'")
                    .put("instance", result.instance)
                    .put("scenarioId", result.scenarioId)
                    .put("container", result.container)
                    .put("agents", agents)
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (IllegalStateException e) {
            ctx.fail(409, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }

    private Map<String, Object> readConfig(JsonObject body) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (body != null && body.containsKey("config")) {
            JsonObject configJson = body.getJsonObject("config");
            if (configJson != null) {
                config.putAll(configJson.getMap());
            }
        }
        return config;
    }
}
