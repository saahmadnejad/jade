package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFStatus;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code GET /api/tools/df-gui/status} — returns DF
 * runtime status (running state, agent AID, container name,
 * registration/federation counts).
 *
 * <p><b>Old GUI implementation:</b>
 * The old DF GUI displayed status in a {@code JTextField} status bar
 * via {@code DFGUI#showStatusMsg()}
 * ({@code io.donbee.jade.tools.dfgui.DFGUI:615}). The counts were
 * derived from {@code DFGUI#refresh()}
 * ({@code io.donbee.jade.tools.dfgui.DFGUI:786}), which populated
 * the {@code registeredModel}, {@code parentModel}, and
 * {@code childrenModel} tables. This REST handler delegates to
 * {@code DFService#getDFStatus()} which returns the same data
 * programmatically.</p>
 */
public class DFStatusHandler implements Handler<RoutingContext> {

    private final DFService service;

    public DFStatusHandler(DFService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            DFStatus status = service.getDFStatus();
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("running", status.running)
                    .put("agent", status.agent)
                    .put("container", status.container)
                    .put("registeredAgentCount", status.registeredAgentCount)
                    .put("parentCount", status.parentCount)
                    .put("childCount", status.childCount)
                    .toBuffer());
        } catch (IllegalStateException e) {
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("running", false).toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
