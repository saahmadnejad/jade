package io.donbee.jade.rest.handler;

import io.donbee.jade.rest.service.DFService;
import io.donbee.jade.rest.service.DFService.DFRegistrationInfo;
import io.donbee.jade.rest.service.DFService.DFServiceInfo;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for {@code GET /api/df/description} — returns the local
 * DF's own {@code DFAgentDescription}.
 *
 * <p><b>Old GUI implementation:</b> The old DF GUI obtained this
 * description via
 * {@code DFGUIAdapter#getDescriptionOfThisDF()}
 * ({@code io.donbee.jade.domain.DFGUIAdapter}). This method returned
 * the {@code DFAgentDescription} that the DF agent exposes about itself.
 * The description was displayed in the "Super DF" / federation tab
 * of {@code io.donbee.jade.tools.dfgui.DFGUI}. This REST handler
 * delegates to {@code DFService#getDFDescription()} which performs the
 * same lookup via a FIPA {@code Search} action.</p>
 */
public class DFDescriptionHandler implements Handler<RoutingContext> {

    private final DFService service;

    public DFDescriptionHandler(DFService service) {
        this.service = service;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            DFRegistrationInfo info = service.getDFDescription();
            List<JsonObject> services = new ArrayList<>();
            if (info.services != null) {
                for (DFServiceInfo svc : info.services) {
                    services.add(new JsonObject()
                        .put("type", svc.type != null ? svc.type : "")
                        .put("name", svc.name != null ? svc.name : "")
                        .put("ownership", svc.ownership != null ? svc.ownership : ""));
                }
            }
            List<String> addrs = info.addresses != null ? new ArrayList<>(info.addresses) : new ArrayList<>();

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("name", info.name != null ? info.name : "")
                    .put("addresses", addrs)
                    .put("services", services)
                    .toBuffer());
        } catch (IllegalStateException e) {
            ctx.fail(403, e);
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
