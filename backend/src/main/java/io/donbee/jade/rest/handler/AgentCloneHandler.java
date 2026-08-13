package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code POST /api/agents/clone}.
 * Clones an agent to a new container (and optionally a new name).
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.CloneAgentAction} invoked the
 * {@code MoveDialog} and then called
 * {@code rma.cloneAgent(agentAid, newAgentName, container)}
 * ({@code rma.java:769}). The old method built a
 * {@link io.donbee.jade.domain.mobility.CloneAction}
 * with a {@code MobileAgentDescription}, encoded it with
 * {@code MobilityOntology}, and sent it to the AMS. This handler
 * delegates to {@code AgentManager#copy()} via
 * {@code JadesPlatformService#cloneAgent()}.</p>
 */
public class AgentCloneHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentCloneHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        RequestBody body = ctx.body();
        String agentName = null;
        String newName = null;
        String targetContainer = null;
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                agentName = json.getString("name");
                newName = json.getString("newName");
                targetContainer = json.getString("container");
            }
        }
        if (agentName == null || agentName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing agent name"));
            return;
        }
        if (newName == null || newName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing newName"));
            return;
        }
        if (targetContainer == null || targetContainer.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing container"));
            return;
        }
        try {
            service.cloneAgent(agentName, newName, targetContainer);
            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "Agent '" + newName + "' cloned from '" + agentName + "'")
                    .put("aid", newName)
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
