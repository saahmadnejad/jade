package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

/**
 * Handler for {@code POST /api/agents/:name/move} — migrate an agent
 * to a different container.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.MoveAgentAction} (labelled
 * "Migrate Agent") invoked the {@code MoveDialog} and then called
 * {@code rma.moveAgent(agentAid, container)}
 * ({@code rma.java:741}). The old method built a
 * {@link io.donbee.jade.domain.mobility.MoveAction} with a
 * {@code MobileAgentDescription}, encoded it with
 * {@code MobilityOntology}, and sent it to the AMS. This handler
 * delegates to {@code AgentManager#move()} via
 * {@code JadesPlatformService#moveAgent()}.</p>
 */
public class AgentMoveHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentMoveHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String agentName = ctx.pathParams().get("name");
        if (agentName == null || agentName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing agent name"));
            return;
        }
        RequestBody body = ctx.body();
        String targetContainer = null;
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                targetContainer = json.getString("container");
            }
        }
        if (targetContainer == null || targetContainer.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing container"));
            return;
        }
        try {
            service.moveAgent(agentName, targetContainer);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Agent '" + agentName + "' moved to '" + targetContainer + "'")
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
