package io.donbee.jade.rest;

import io.donbee.jade.core.AgentContainer;
import io.donbee.jade.core.AgentManager;
import io.donbee.jade.core.MainContainer;
import io.donbee.jade.rest.handler.AgentActionHandler;
import io.donbee.jade.rest.handler.AgentCloneHandler;
import io.donbee.jade.rest.handler.AgentDeployHandler;
import io.donbee.jade.rest.handler.AgentFreezeThawHandler;
import io.donbee.jade.rest.handler.AgentInfoHandler;
import io.donbee.jade.rest.handler.AgentListHandler;
import io.donbee.jade.rest.handler.AgentMoveHandler;
import io.donbee.jade.rest.handler.AgentOwnershipHandler;
import io.donbee.jade.rest.handler.AgentSaveLoadHandler;
import io.donbee.jade.rest.handler.ContainerInfoHandler;
import io.donbee.jade.rest.handler.ContainerKillHandler;
import io.donbee.jade.rest.handler.ContainerListHandler;
import io.donbee.jade.rest.handler.ContainerLoadHandler;
import io.donbee.jade.rest.handler.ContainerMTPInstallHandler;
import io.donbee.jade.rest.handler.ContainerMTPListHandler;
import io.donbee.jade.rest.handler.ContainerMTPUNinstallHandler;
import io.donbee.jade.rest.handler.ContainerSaveHandler;
import io.donbee.jade.rest.handler.PlatformInfoHandler;
import io.donbee.jade.rest.handler.RemotePlatformAddHandler;
import io.donbee.jade.rest.handler.RemotePlatformAgentsHandler;
import io.donbee.jade.rest.handler.RemotePlatformListHandler;
import io.donbee.jade.rest.handler.ShutdownHandler;
import io.donbee.jade.rest.handler.ToolLaunchHandler;
import io.donbee.jade.rest.handler.HealthHandler;
import io.donbee.jade.rest.handler.JsonFailureHandler;
import io.donbee.jade.rest.handler.PlatformInfoHandler;
import io.donbee.jade.rest.handler.ShutdownHandler;
import io.donbee.jade.rest.handler.VersionHandler;
import io.donbee.jade.rest.service.JadesPlatformService;
import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
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
        PlatformService service = buildService();

        Router router = Router.router(vertx);
        configureMiddleware(router);
        configureRoutes(router, service);

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

    /**
     * Build the PlatformService by extracting internal JADE objects
     * from the wrapper container.
     */
    private PlatformService buildService() {
        AgentContainer impl = extractImpl();
        AgentManager agentManager = extractAgentManager(impl);
        return new JadesPlatformService(impl, agentManager);
    }

    /**
     * Configure middleware: CORS, body parsing, and error handling.
     */
    private void configureMiddleware(Router router) {
        router.route().handler(CorsHandler.create().addOrigin("*"));
        router.route().handler(BodyHandler.create());
        router.route().failureHandler(new JsonFailureHandler());
    }

    /**
     * Configure all REST API routes, mapping paths to handlers.
     */
    private void configureRoutes(Router router, PlatformService service) {
        // Health
        router.get(ApiRoutes.HEALTH).handler(new HealthHandler());

        // Version
        router.get(ApiRoutes.VERSION).handler(new VersionHandler());

        // Platform
        router.get(ApiRoutes.PLATFORM).handler(new PlatformInfoHandler(service));
        router.post(ApiRoutes.PLATFORM_SHUTDOWN).handler(new ShutdownHandler(service));

        // Containers
        router.get(ApiRoutes.CONTAINERS).handler(new ContainerListHandler(service));
        router.get(ApiRoutes.CONTAINER_BY_NAME).handler(new ContainerInfoHandler(service));
        router.delete(ApiRoutes.CONTAINER_BY_NAME).handler(new ContainerKillHandler(service));
        router.post(ApiRoutes.CONTAINER_BY_NAME_SAVE).handler(new ContainerSaveHandler(service));
        router.post(ApiRoutes.CONTAINER_BY_NAME_LOAD).handler(new ContainerLoadHandler(service));
        router.post(ApiRoutes.CONTAINER_MTPS).handler(new ContainerMTPInstallHandler(service));
        router.get(ApiRoutes.CONTAINER_MTPS).handler(new ContainerMTPListHandler(service));
        router.delete(ApiRoutes.CONTAINER_MTP_BY_ADDRESS).handler(new ContainerMTPUNinstallHandler(service));

        // Agents
        router.get(ApiRoutes.AGENTS).handler(new AgentListHandler(service));
        router.get(ApiRoutes.AGENT_BY_NAME).handler(new AgentInfoHandler(service));
        router.post(ApiRoutes.AGENTS).handler(new AgentDeployHandler(service));
        router.delete(ApiRoutes.AGENT_BY_NAME).handler(
            new AgentActionHandler(service, AgentActionHandler.Action.KILL));
        router.post(ApiRoutes.AGENT_SUSPEND).handler(
            new AgentActionHandler(service, AgentActionHandler.Action.SUSPEND));
        router.post(ApiRoutes.AGENT_RESUME).handler(
            new AgentActionHandler(service, AgentActionHandler.Action.RESUME));
        router.post(ApiRoutes.AGENT_FREEZE).handler(new AgentFreezeThawHandler(service, true));
        router.post(ApiRoutes.AGENT_THAW).handler(new AgentFreezeThawHandler(service, false));
        router.post(ApiRoutes.AGENT_CLONE).handler(new AgentCloneHandler(service));
        router.post(ApiRoutes.AGENT_MOVE).handler(new AgentMoveHandler(service));
        router.post(ApiRoutes.AGENT_SAVE).handler(new AgentSaveLoadHandler(service, true));
        router.post(ApiRoutes.AGENT_LOAD).handler(new AgentSaveLoadHandler(service, false));
        router.patch(ApiRoutes.AGENT_BY_NAME).handler(new AgentOwnershipHandler(service));

        // Tools
        router.post(ApiRoutes.TOOLS_START).handler(new ToolLaunchHandler(service));

        // Remote Platforms
        router.get(ApiRoutes.PLATFORMS).handler(new RemotePlatformListHandler(service));
        router.post(ApiRoutes.PLATFORMS).handler(new RemotePlatformAddHandler(service));
        router.get(ApiRoutes.PLATFORM_AGENTS).handler(new RemotePlatformAgentsHandler(service));
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
