package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for deploying a new agent via {@code POST /api/agents}.
 * Expected request body: {@code {"name": "...", "class": "...", "args": [...]}}
 * Returns 201 with the agent's info.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.StartNewAgentAction} invoked the
 * {@code StartDialog} and then called
 * {@code rma.newAgent(agentName, className, args, owner, container)}
 * ({@code rma.java:475}). The old method built a
 * {@link io.donbee.jade.domain.JADEAgentManagement.CreateAgent}
 * action, set owner/credentials via the {@code SecurityService},
 * encoded it with {@code JADEManagementOntology}, and sent it to the
 * AMS via an {@code AMSClientBehaviour}. This handler delegates to
 * {@code AgentManager#create()} through
 * {@code JadesPlatformService#deployAgent()}.</p>
 */
public class AgentDeployHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public AgentDeployHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.fail(400, new RuntimeException("Request body is required"));
            return;
        }
        String name = body.getString("name");
        String className = body.getString("class");
        if (name == null || name.isEmpty() || className == null || className.isEmpty()) {
            ctx.fail(400, new RuntimeException("Both 'name' and 'class' are required"));
            return;
        }
        Object[] args;
        if (body.getJsonArray("args") != null) {
            args = body.getJsonArray("args").getList().toArray();
        } else {
            args = new Object[0];
        }
        try {
            PlatformService.AgentInfo agent = service.deployAgent(name, className, args);
            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "Agent '" + name + "' deployed")
                    .put("agent", new JsonObject()
                        .put("name", agent.name)
                        .put("state", "UNKNOWN")
                        .put("container", agent.container))
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(409, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
