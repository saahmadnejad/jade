package io.donbee.jade.rest;

import io.donbee.jade.core.AgentContainer;
import io.donbee.jade.core.AgentManager;
import io.donbee.jade.core.MainContainer;
import io.donbee.jade.rest.handler.AgentActionHandler;
import io.donbee.jade.rest.handler.AgentInfoHandler;
import io.donbee.jade.rest.handler.AgentListHandler;
import io.donbee.jade.rest.handler.ContainerListHandler;
import io.donbee.jade.rest.handler.HealthHandler;
import io.donbee.jade.rest.handler.PlatformInfoHandler;
import io.donbee.jade.rest.handler.ShutdownHandler;
import io.donbee.jade.rest.handler.VersionHandler;
import io.donbee.jade.rest.service.JadesPlatformService;
import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.lang.reflect.Field;

/**
 * Vert.x REST verticle that wires all route handlers.
 * This class has a single responsibility: configure the HTTP router.
 */
public class RestAPIVerticle extends AbstractVerticle {

    private final io.donbee.jade.wrapper.AgentContainer wrapper;
    private final int port;

    public RestAPIVerticle(io.donbee.jade.wrapper.AgentContainer container) {
        this(container, 8080);
    }

    public RestAPIVerticle(io.donbee.jade.wrapper.AgentContainer container, int port) {
        this.wrapper = container;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        AgentContainer impl = extractImpl();
        AgentManager agentManager = extractAgentManager(impl);
        PlatformService service = new JadesPlatformService(impl, agentManager);

        Router router = Router.router(vertx);
        router.route().handler(CorsHandler.create().addOrigin("*"));
        router.route().handler(BodyHandler.create());
        router.route().failureHandler(routingContext -> {
            int statusCode = routingContext.statusCode();
            if (statusCode < 400) {
                statusCode = 500;
            }
            String message = routingContext.failure() != null
                ? routingContext.failure().getMessage()
                : "Unexpected error";
            routingContext.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", message).put("code", statusCode).encode());
        });

        // Health
        router.get("/api/health").handler(new HealthHandler());

        // Version
        router.get("/api/version").handler(new VersionHandler());

        // Platform info
        router.get("/api/platform").handler(new PlatformInfoHandler(service));

        // Platform shutdown
        router.post("/api/platform/shutdown").handler(new ShutdownHandler(service));

        // Containers
        router.get("/api/containers").handler(new ContainerListHandler(service));

        // Agents
        router.get("/api/agents").handler(new AgentListHandler(service));
        router.get("/api/agents/:name").handler(new AgentInfoHandler(service));
        router.delete("/api/agents/:name").handler(new AgentActionHandler(service, AgentActionHandler.Action.KILL));
        router.post("/api/agents/:name/suspend").handler(new AgentActionHandler(service, AgentActionHandler.Action.SUSPEND));
        router.post("/api/agents/:name/resume").handler(new AgentActionHandler(service, AgentActionHandler.Action.RESUME));

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port)
            .onComplete(http -> {
                if (http.succeeded()) {
                    startPromise.complete();
                } else {
                    startPromise.fail(http.cause());
                }
            });
    }

    private AgentContainer extractImpl() {
        try {
            Field f = io.donbee.jade.wrapper.ContainerController.class.getDeclaredField("myImpl");
            f.setAccessible(true);
            return (AgentContainer) f.get(wrapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access internal container", e);
        }
    }

    private AgentManager extractAgentManager(AgentContainer impl) {
        try {
            MainContainer main = impl.getMain();
            if (main != null) {
                return (AgentManager) main;
            }
        } catch (Exception e) {
            // Not a main container
        }
        return null;
    }
}
