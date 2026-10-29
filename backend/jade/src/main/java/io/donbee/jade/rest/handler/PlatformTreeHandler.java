package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.donbee.jade.rest.service.PlatformService;

/**
 * Handler for the platform-tree endpoint ({@code GET /api/platform/tree}).
 * Returns the full hierarchical platform tree: platform -> containers -> agents
 * and registered remote platforms.
 *
 * <p><b>Old GUI implementation:</b>
 * In the legacy Swing GUI, the platform tree was rendered by
 * {@code io.donbee.jade.gui.AgentTree} backed by {@code AgentTreeModel}
 * inside {@code io.donbee.jade.tools.rma.MainWindow}. Containers and agents
 * were dynamically maintained through AMS introspection events
 * handled by {@code rma.AMSListenerBehaviour} ({@code io.donbee.jade.tools.rma.rma:170}).
 * This handler delegates to {@code PlatformService#getPlatformTree()}
 * to produce the full hierarchical tree for the React UI.</p>
 */
public class PlatformTreeHandler implements Handler<RoutingContext> {

    private final PlatformService service;

    public PlatformTreeHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }

        ctx.vertx().executeBlocking(() -> {
            return service.getPlatformTree();
        }).onSuccess(tree -> {
            JsonObject root = new JsonObject();
            root.put("platformID", tree.platformID);

            JsonArray containersArr = new JsonArray();
            for (PlatformService.ContainerTreeNode c : tree.containers) {
                JsonObject cObj = new JsonObject()
                    .put("name", c.name)
                    .put("address", c.address)
                    .put("port", c.port)
                    .put("isMain", c.isMain);

                JsonArray agentsArr = new JsonArray();
                for (PlatformService.AgentInfo a : c.agents) {
                    agentsArr.add(new JsonObject()
                        .put("name", a.name)
                        .put("state", a.state != null ? a.state : "UNKNOWN")
                        .put("ownership", a.ownership != null ? a.ownership : "")
                        .put("container", a.container)
                        .put("addresses", jsonFromArray(a.addresses)));
                }
                cObj.put("agents", agentsArr);
                containersArr.add(cObj);
            }
            root.put("containers", containersArr);

            JsonArray remoteArr = new JsonArray();
            for (PlatformService.RemotePlatformInfo rp : tree.remotePlatforms) {
                remoteArr.add(new JsonObject()
                    .put("name", rp.name)
                    .put("ams", rp.ams)
                    .put("addresses", jsonFromArray(rp.addresses))
                    .put("services", jsonFromArray(rp.services)));
            }
            root.put("remotePlatforms", remoteArr);

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(root.toBuffer());
        }).onFailure(err -> ctx.fail(500, err));
    }

    private JsonArray jsonFromArray(String[] arr) {
        JsonArray j = new JsonArray();
        if (arr != null) {
            for (String s : arr) {
                j.add(s);
            }
        }
        return j;
    }
}
