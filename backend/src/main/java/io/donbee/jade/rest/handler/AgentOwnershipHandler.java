package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code PATCH /api/agents/:name} — change agent ownership.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.ChangeAgentOwnershipAction}
 * invoked a {@code PwdDialog} and then called
 * {@code rma.changeAgentOwnership(agentID, owner)}
 * ({@code rma.java:611}), which sent a
 * {@link io.donbee.jade.domain.FIPAAgentManagement.Modify} action with
 * {@code AMSAgentDescription.ownership} set to the new owner, via
 * {@code FIPAManagementOntology}. This handler delegates to
 * {@code JadesPlatformService#changeAgentOwnership()} which replicates
 * the same ACL messaging.</p>
 */
public class AgentOwnershipHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentOwnershipHandler(PlatformService service) {
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
        String newOwner = null;
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                newOwner = json.getString("ownership");
                if (newOwner == null) {
                    newOwner = json.getString("newOwner");
                }
            }
        }
        if (newOwner == null || newOwner.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing ownership"));
            return;
        }
        try {
            service.changeAgentOwnership(agentName, newOwner);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Agent '" + agentName + "' ownership changed to '" + newOwner + "'")
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
