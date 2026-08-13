package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.PlatformService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code POST /api/tools/{tool}/start} — launch a GUI
 * tool agent (sniffer, dummy, logger, introspector, or df-gui).
 * Tool agents are started using the same CreateAgent mechanism as
 * regular agent deployment.
 *
 * <p><b>Old GUI implementation:</b>
 * Tool launch was handled by dedicated Swing action classes in
 * {@code io.donbee.jade.tools.rma}, each calling
 * {@code rma.newAgent(...)} ({@code rma.java:475}):
 * <ul>
 *   <li><b>Sniffer:</b> {@code io.donbee.jade.tools.rma.SnifferAction}
 *       — deployed {@code io.donbee.jade.tools.sniffer.Sniffer}.</li>
 *   <li><b>DummyAgent:</b> {@code io.donbee.jade.tools.rma.DummyAgentAction}
 *       — deployed {@code io.donbee.jade.tools.DummyAgent.DummyAgent}.</li>
 *   <li><b>Logger:</b> {@code io.donbee.jade.tools.rma.LogManagerAgentAction}
 *       — deployed {@code io.donbee.jade.tools.logging.LogManagerAgent}.</li>
 *   <li><b>Introspector:</b> {@code io.donbee.jade.tools.rma.IntrospectorAction}
 *       — deployed {@code io.donbee.jade.tools.introspector.Introspector}.</li>
 *   <li><b>DF GUI:</b> {@code io.donbee.jade.tools.rma.ShowDFGuiAction}
 *       — sent a {@link io.donbee.jade.domain.JADEAgentManagement.ShowGui}
 *       action to the DF (instead of creating a new agent).</li>
 * </ul>
 * This handler uses {@code AgentManager#create()} (same as
 * {@code rma.newAgent()}) for all tools. Note: the old
 * {@code ShowDFGuiAction} sent a {@code ShowGui} action rather than
 * creating a new agent; that path is not yet replicated here (see
 * {@code io.donbee.jade.tools.dfgui.DFGUI}).</p>
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
