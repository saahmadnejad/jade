package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for individual agent lifecycle actions: kill, suspend, resume
 * ({@code DELETE /api/agents/:name},
 * {@code POST /api/agents/:name/suspend},
 * {@code POST /api/agents/:name/resume}).
 *
 * <p><b>Old GUI implementation:</b>
 * <ul>
 *   <li><b>Kill:</b> {@code io.donbee.jade.tools.rma.KillAction}
 *       ({@code doAction(AgentNode)}) called
 *       {@code rma.killAgent(id)} ({@code rma.java:644}), which sent a
 *       {@link io.donbee.jade.domain.JADEAgentManagement.KillAgent}
 *       action to the AMS via {@code AMSClientBehaviour}.</li>
 *   <li><b>Suspend:</b> {@code io.donbee.jade.tools.rma.SuspendAction}
 *       called {@code rma.suspendAgent(id)} ({@code rma.java:552}),
 *       which sent a {@link io.donbee.jade.domain.FIPAAgentManagement.Modify}
 *       action with {@code AMSAgentDescription.state = SUSPENDED} via
 *       {@code FIPAManagementOntology}.</li>
 *   <li><b>Resume:</b> {@code io.donbee.jade.tools.rma.ResumeAction}
 *       called {@code rma.resumeAgent(id)} ({@code rma.java:586}),
 *       which sent a {@code Modify} action with
 *       {@code state = ACTIVE}.</li>
 * </ul>
 * This handler delegates to {@code AgentManager#kill()},
 * {@code #suspend()}, and {@code #activate()} in the service layer.</p>
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
