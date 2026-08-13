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
 * Handler for agent freeze/thaw:
 * {@code POST /api/agents/:name/freeze} and
 * {@code POST /api/agents/:name/thaw}.
 *
 * <p><b>Old GUI implementation:</b>
 * <ul>
 *   <li><b>Freeze:</b> {@code io.donbee.jade.tools.rma.FreezeAgentAction}
 *       called {@code rma.freezeAgent(agentAid, "JADE-DB")}
 *       ({@code rma.java:849}), which sent a
 *       {@link io.donbee.jade.domain.persistence.FreezeAgent} action
 *       via {@code PersistenceOntology}. The old action did not collect
 *       a buffer-container name from the user; it only asked for a
 *       repository.</li>
 *   <li><b>Thaw:</b> {@code io.donbee.jade.tools.rma.ThawAgentAction}
 *       called {@code rma.thawAgent(agentAid, "JADE-DB", newContainer)}
 *       ({@code rma.java:873}), which sent a
 *       {@link io.donbee.jade.domain.persistence.ThawAgent} action
 *       via {@code PersistenceOntology}. The old action prompted for
 *       the target container via {@code JOptionPane.showInputDialog}.</li>
 * </ul>
 * This handler accepts both {@code container} and {@code repository}
 * in the JSON body (an enhancement over the old hardcoded repository).
 * The service-layer methods ({@code JadesPlatformService#freezeAgent()},
 * {@code #thawAgent()}) replicate the same AMS ACL messaging via
 * {@code PersistenceOntology}.</p>
 */
public class AgentFreezeThawHandler implements Handler<RoutingContext> {
    private final PlatformService service;
    private final boolean freeze;

    public AgentFreezeThawHandler(PlatformService service, boolean freeze) {
        this.service = service;
        this.freeze = freeze;
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
        String container = null;
        String repository = null;
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                container = json.getString("container");
                repository = json.getString("repository");
            }
        }
        if (container == null || container.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing container"));
            return;
        }
        if (repository == null || repository.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing repository"));
            return;
        }
        try {
            if (freeze) {
                service.freezeAgent(agentName, container, repository);
            } else {
                service.thawAgent(agentName, container, repository);
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message",
                    (freeze ? "Agent '" + agentName + "' frozen to '" + container + "'"
                              : "Agent '" + agentName + "' thawed to '" + container + "'"))
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
