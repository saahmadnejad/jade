package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for individual agent lifecycle actions: kill, suspend, resume.
 */
public class AgentActionHandler implements Handler<RoutingContext> {
    private final PlatformService service;
    private final Action action;

    public enum Action { KILL, SUSPEND, RESUME }

    public AgentActionHandler(PlatformService service, Action action) {
        this.service = service;
        this.action = action;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String agentName = ctx.pathParams().get("name");
        if (agentName == null) {
            ctx.fail(400, new RuntimeException("Missing agent name"));
            return;
        }
        // Strip container suffix if present (e.g. "agent@container" -> "agent")
        if (agentName.contains("@")) {
            agentName = agentName.substring(0, agentName.indexOf('@'));
        }
        try {
            String msg;
            switch (action) {
                case KILL:
                    service.killAgent(agentName);
                    msg = "Agent '" + agentName + "' killed";
                    ctx.response().setStatusCode(200);
                    break;
                case SUSPEND:
                    service.suspendAgent(agentName);
                    msg = "Agent '" + agentName + "' suspended";
                    ctx.response().setStatusCode(200);
                    break;
                case RESUME:
                    service.resumeAgent(agentName);
                    msg = "Agent '" + agentName + "' resumed";
                    ctx.response().setStatusCode(200);
                    break;
                default:
                    ctx.fail(400, new RuntimeException("Unknown action: " + action));
                    return;
            }
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", msg).toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
