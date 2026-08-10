package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for POST /api/agents/:name/save and POST /api/agents/load.
 */
public class AgentSaveLoadHandler implements Handler<RoutingContext> {
    private final PlatformService service;
    private final boolean save;

    public AgentSaveLoadHandler(PlatformService service, boolean save) {
        this.service = service;
        this.save = save;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        RequestBody body = ctx.body();
        String repository = null;
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                repository = json.getString("repository");
            }
        }
        if (repository == null || repository.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing repository"));
            return;
        }
        try {
            if (save) {
                String agentName = ctx.pathParams().get("name");
                if (agentName == null || agentName.isEmpty()) {
                    ctx.fail(400, new RuntimeException("Missing agent name"));
                    return;
                }
                service.saveAgent(agentName, repository);
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("message", "Agent '" + agentName + "' saved to '" + repository + "'")
                        .toBuffer());
            } else {
                String agentName = ctx.body().asJsonObject().getString("name");
                String targetContainer = ctx.body().asJsonObject().getString("container");
                if (agentName == null || agentName.isEmpty()) {
                    ctx.fail(400, new RuntimeException("Missing agent name"));
                    return;
                }
                if (targetContainer == null || targetContainer.isEmpty()) {
                    ctx.fail(400, new RuntimeException("Missing container"));
                    return;
                }
                service.loadAgent(agentName, targetContainer, repository);
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("message", "Agent '" + agentName + "' loaded from '" + repository + "' to '" + targetContainer + "'")
                        .toBuffer());
            }
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
