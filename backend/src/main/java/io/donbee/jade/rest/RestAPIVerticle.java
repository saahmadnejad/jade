package io.donbee.jade.rest;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.AgentContainer;
import io.donbee.jade.core.AgentManager;
import io.donbee.jade.core.ContainerID;
import io.donbee.jade.core.MainContainer;
import io.donbee.jade.core.VersionManager;
import io.donbee.jade.domain.FIPAAgentManagement.AMSAgentDescription;
import io.donbee.jade.util.leap.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.lang.reflect.Field;

public class RestAPIVerticle extends AbstractVerticle {

    private final io.donbee.jade.wrapper.AgentContainer wrapper;
    private final int port;
    private AgentContainer impl;
    private AgentManager agentManager;

    public RestAPIVerticle(io.donbee.jade.wrapper.AgentContainer container) {
        this(container, 8080);
    }

    public RestAPIVerticle(io.donbee.jade.wrapper.AgentContainer container, int port) {
        this.wrapper = container;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            Field f = io.donbee.jade.wrapper.ContainerController.class.getDeclaredField("myImpl");
            f.setAccessible(true);
            impl = (AgentContainer) f.get(wrapper);
            MainContainer main = impl.getMain();
            if (main != null) {
                agentManager = (AgentManager) main;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to access internal container", e);
        }

        Router router = Router.router(vertx);

        router.route().handler(CorsHandler.create("*"));
        router.route().handler(BodyHandler.create());

        router.get("/api/health").handler(ctx -> {
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("status", "ok").toBuffer());
        });

        router.get("/api/version").handler(ctx -> {
            JsonObject resp = new JsonObject()
                .put("version", "UNKNOWN")
                .put("revision", "UNKNOWN")
                .put("date", "UNKNOWN");
            VersionManager vm = new VersionManager();
            try {
                resp.put("version", vm.getVersion());
                resp.put("revision", vm.getRevision());
                resp.put("date", vm.getDate());
            } catch (Exception ignored) {}
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(resp.toBuffer());
        });

        router.get("/api/platform").handler(ctx -> {
            try {
                JsonObject resp = new JsonObject()
                    .put("platformID", impl.getPlatformID())
                    .put("containerName", impl.here().getName())
                    .put("isMain", impl.getMain() != null)
                    .put("ams", impl.getAMS().getName())
                    .put("defaultDF", impl.getDefaultDF().getName());
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(resp.toBuffer());
            } catch (Exception e) {
                ctx.fail(500, e);
            }
        });

        router.get("/api/agents").handler(ctx -> {
            if (agentManager == null) {
                ctx.fail(403, new RuntimeException("Not a Main Container"));
                return;
            }
            try {
                boolean detail = "true".equalsIgnoreCase(ctx.queryParams().get("detail"));
                ContainerID cid = impl.getID();
                List agents = agentManager.containerAgents(cid);
                JsonArray arr = new JsonArray();
                for (int i = 0; i < agents.size(); i++) {
                    AID aid = (AID) agents.get(i);
                    if (detail) {
                        try {
                            AMSAgentDescription amsDesc = agentManager.getAMSDescription(aid);
                            JsonObject agentObj = new JsonObject()
                                .put("name", aid.getName())
                                .put("state", amsDesc.getState() != null ? amsDesc.getState() : "UNKNOWN")
                                .put("ownership", amsDesc.getOwnership() != null ? amsDesc.getOwnership() : "")
                                .put("container", cid.getName())
                                .put("addresses", aid.getAllAddresses() != null ? toJsonArray(aid.getAllAddresses()) : new JsonArray());
                            arr.add(agentObj);
                        } catch (Exception ignored) {
                            arr.add(new JsonObject().put("name", aid.getName()));
                        }
                    } else {
                        arr.add(new JsonObject().put("name", aid.getName()));
                    }
                }
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("agents", arr).toBuffer());
            } catch (Exception e) {
                ctx.fail(500, e);
            }
        });

        router.get("/api/containers").handler(ctx -> {
            if (agentManager == null) {
                ctx.fail(403, new RuntimeException("Not a Main Container"));
                return;
            }
            try {
                ContainerID[] cids = agentManager.containerIDs();
                JsonArray arr = new JsonArray();
                for (ContainerID cid : cids) {
                    JsonObject containerObj = new JsonObject()
                        .put("name", cid.getName())
                        .put("address", cid.getAddress() != null ? cid.getAddress() : "")
                        .put("port", cid.getPort() != null ? cid.getPort() : "")
                        .put("isMain", cid.getName() != null && cid.getName().equals(impl.here().getName()));
                    arr.add(containerObj);
                }
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("containers", arr).toBuffer());
            } catch (Exception e) {
                ctx.fail(500, e);
            }
        });

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port, http -> {
                if (http.succeeded()) {
                    startPromise.complete();
                } else {
                    startPromise.fail(http.cause());
                }
            });
    }

    private static JsonArray toJsonArray(java.util.Iterator<String> it) {
        JsonArray arr = new JsonArray();
        while (it.hasNext()) {
            arr.add(it.next());
        }
        return arr;
    }
}
