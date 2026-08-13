package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code POST /api/platforms} — add a remote platform
 * via AMS AID.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.AddRemotePlatformAction} invoked
 * an {@code AIDGui} dialog and then called
 * {@code rma.addRemotePlatform(remoteAMS)} ({@code rma.java:976}),
 * which sent a {@link io.donbee.jade.domain.FIPAAgentManagement.GetDescription}
 * action to the remote AMS to retrieve the
 * {@code APDescription}. The reply was handled by
 * {@code handleAddRemotePlatformBehaviour}
 * ({@code rma.java:111}) and displayed in the
 * {@code MainWindow} tree. This handler delegates to
 * {@code JadesPlatformService#addRemotePlatform()}.</p>
 */
public class RemotePlatformAddHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public RemotePlatformAddHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        RequestBody body = ctx.body();
        String amsName = null;
        String[] addresses = {};
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                amsName = json.getString("ams");
                if (json.getJsonArray("addresses") != null) {
                    java.util.List<String> addrList = json.getJsonArray("addresses").getList();
                    addresses = addrList.toArray(new String[0]);
                }
            }
        }
        if (amsName == null || amsName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing AMS AID"));
            return;
        }
        try {
            PlatformService.RemotePlatformInfo platform = service.addRemotePlatform(amsName, addresses);
            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "Platform '" + platform.name + "' added")
                    .put("platform", new JsonObject()
                        .put("name", platform.name)
                        .put("ams", platform.ams)
                        .put("addresses", new io.vertx.core.json.JsonArray(java.util.Arrays.asList(platform.addresses))))
                    .toBuffer());
        } catch (Exception e) {
            ctx.fail(400, new RuntimeException("Invalid AMS or platform unreachable: " + e.getMessage()));
        }
    }
}
