package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code POST /api/containers/:name/mtps} — install an MTP
 * (Message Transport Protocol) on a container.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.InstallMTPAction} invoked
 * {@code rma.installMTP(containerName)} ({@code rma.java:927}), which
 * showed a Swing {@code InstallMTPDialog} to gather arguments, then sent
 * an {@link io.donbee.jade.domain.JADEAgentManagement.InstallMTP}
 * action to the AMS. This handler accepts the same {@code className}
 * and {@code address} via JSON body and delegates to
 * {@code AgentManager#installMTP()} (see
 * {@code JadesPlatformService#installMTP()}).</p>
 */
public class ContainerMTPInstallHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public ContainerMTPInstallHandler(PlatformService service) {
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
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.fail(400, new RuntimeException("Missing request body"));
            return;
        }
        String className = body.getString("className");
        String address = body.getString("address");
        if (className == null || className.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing className"));
            return;
        }
        if (address == null || address.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing address"));
            return;
        }
        try {
            service.installMTP(containerName, address, className);
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "MTP installed on container '" + containerName + "'")
                    .put("address", address)
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, new RuntimeException("MTP installation failed: " + e.getMessage()));
        }
    }
}
