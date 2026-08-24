package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * Handler for {@code GET /api/platforms/:name/agents} — list agents
 * on a remote platform.
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.rma.RefreshAMSAgentAction} called
 * {@code rma.refreshRemoteAgent(apDesc, amsAID)}
 * ({@code rma.java:1080}), which sent a
 * {@link io.donbee.jade.domain.FIPAAgentManagement.Search} action
 * to the remote AMS (with an empty {@code AMSAgentDescription} and
 * {@code maxResults = -1}) via an
 * {@code handleRefreshRemoteAgentBehaviour}
 * ({@code rma.java:138}). The results were displayed as
 * {@code RemoteAgentNode} entries in the {@code MainWindow} tree.
 * This handler delegates to
 * {@code JadesPlatformService#searchRemotePlatformAgents()}.</p>
 */
public class RemotePlatformAgentsHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public RemotePlatformAgentsHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String platformName = ctx.pathParams().get("name");
        if (platformName == null || platformName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing platform name"));
            return;
        }
        try {
            List<PlatformService.AgentInfo> agents = service.searchRemotePlatformAgents(platformName);
            JsonArray arr = new JsonArray();
            for (PlatformService.AgentInfo a : agents) {
                arr.add(new JsonObject()
                    .put("name", a.name)
                    .put("addresses", new JsonArray(java.util.Arrays.asList(a.addresses))));
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("agents", arr).toBuffer());
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
