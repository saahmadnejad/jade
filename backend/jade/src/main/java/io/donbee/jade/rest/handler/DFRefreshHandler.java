package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for {@code POST /api/df/refresh} — refresh all DF data
 * (registrations, federation counts).
 *
 * <p><b>Old GUI implementation:</b>
 * {@code io.donbee.jade.tools.dfgui.DFGUIRefreshAppletAction}
 * triggered {@code DFGUI#refresh()}
 * ({@code io.donbee.jade.tools.dfgui.DFGUI:786}), which accepted three
 * {@code Iterator}s — registered agents, parent DFs, and child DFs —
 * and repopulated the three table models
 * ({@code registeredModel}, {@code parentModel}, {@code childrenModel}).
 * This REST handler delegates to
 * {@code DFService#getDFStatus()} which performs equivalent
 * re-queries (search + get-parents + children enumeration).</p>
 */
public class DFRefreshHandler implements Handler<RoutingContext> {

    private final DFService service;

    public DFRefreshHandler(DFService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            var status = service.getDFStatus();
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("message", "DF refreshed")
                    .put("registeredAgentCount", status.registeredAgentCount)
                    .put("parentCount", status.parentCount)
                    .put("childCount", status.childCount)
                    .toBuffer());
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
