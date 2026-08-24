package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for agent persistence:
 * {@code POST /api/agents/:name/save} and
 * {@code POST /api/agents/load}.
 *
 * <p><b>Old GUI implementation:</b>
 * <ul>
 *   <li><b>Save:</b> {@code io.donbee.jade.tools.rma.SaveAgentAction}
 *       called {@code rma.saveAgent(agentAid, "JADE-DB")}
 *       ({@code rma.java:798}), which sent a
 *       {@link io.donbee.jade.domain.persistence.SaveAgent} action
 *       via {@code PersistenceOntology} to the AMS.</li>
 *   <li><b>Load:</b> {@code io.donbee.jade.tools.rma.LoadAgentAction}
 *       called {@code rma.loadAgent(agentAid, "JADE-DB", container)}
 *       ({@code rma.java:822}), which sent a
 *       {@link io.donbee.jade.domain.persistence.LoadAgent} action
 *       via {@code PersistenceOntology}. The old action used an
 *       {@code AIDGui} dialog to collect the agent AID; this handler
 *       accepts the name as a JSON field instead.</li>
 * </ul>
 * Both old actions hardcoded {@code "JADE-DB"} as the repository;
 * this handler accepts a caller-supplied repository. The service-layer
 * methods ({@code JadesPlatformService#saveAgent()},
 * {@code #loadAgent()}) replicate the same AMS ACL messaging.</p>
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
