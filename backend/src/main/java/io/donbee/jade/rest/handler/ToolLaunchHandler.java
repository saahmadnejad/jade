package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for POST /api/tools/{tool}/start — launch a GUI tool agent.
 * Tool agents are started using the same CreateAgent mechanism as regular agent deployment.
 */
public class ToolLaunchHandler implements Handler<RoutingContext> {
    private final PlatformService service;

    public ToolLaunchHandler(PlatformService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!service.isMainContainer()) {
            ctx.fail(403, new RuntimeException("Not a Main Container"));
            return;
        }
        String toolName = ctx.pathParams().get("tool");
        if (toolName == null || toolName.isEmpty()) {
            ctx.fail(400, new RuntimeException("Missing tool name"));
            return;
        }
        String className = mapToolClassName(toolName);
        if (className == null) {
            ctx.fail(400, new RuntimeException("Unknown tool: " + toolName));
            return;
        }
        RequestBody body = ctx.body();
        String container = null;
        if (body != null) {
            JsonObject json = body.asJsonObject();
            if (json != null) {
                container = json.getString("container");
            }
        }
        if (container == null || container.isEmpty()) {
            container = "Main-Container";
        }
        try {
            String agentName = toolName + "-agent";
            PlatformService.AgentInfo info = service.deployAgent(agentName, className, new Object[]{});
            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", toolName + " started")
                    .put("agent", info.name)
                    .toBuffer());
        } catch (IllegalArgumentException e) {
            ctx.fail(404, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }

    private String mapToolClassName(String toolName) {
        switch (toolName) {
            case "sniffer":
                return "io.donbee.jade.tools.sniffer.Sniffer";
            case "dummy":
                return "io.donbee.jade.tools.DummyAgent.DummyAgent";
            case "logger":
                return "io.donbee.jade.tools.logging.LoggerAgent";
            case "introspector":
                return "io.donbee.jade.tools.introspector.Introspector";
            case "df-gui":
                return "io.donbee.jade.tools.df.DFTool";
            default:
                return null;
        }
    }
}
