package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for deleting a container via {@code DELETE /api/containers/:name}.
 * The Main Container cannot be killed (use {@link ShutdownHandler} instead).
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.KillAction} dispatched container kills
 * from the tree's popup menu ({@code PopupMenuContainer}). When invoked
 * on a container node, it called
 * {@code rma.killContainer(name)} ({@code rma.java:716}), which built
 * a {@link io.donbee.jade.domain.JADEAgentManagement.KillContainer}
 * action and sent it to the AMS via an
 * {@code AMSClientBehaviour}. This handler delegates to
 * {@code PlatformService#killContainer()} which uses
 * {@code AgentManager#killContainer()} in the service layer.</p>
 */
public class ContainerKillHandler implements io.vertx.core.Handler<RoutingContext> {
    private final PlatformService service;

    public ContainerKillHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String containerName = ctx.pathParams().get("name");
        if (containerName == null || containerName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing container name"));
            return;
        }
        String confirm = ctx.queryParams().get("confirm");
        if (!"true".equals(confirm)) {
            ctx.fail(400, new RuntimeException("Set query param confirm=true to confirm"));
            return;
        }
        try {
            service.killContainer(containerName);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("message", "Container '" + containerName + "' killed").encode());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
