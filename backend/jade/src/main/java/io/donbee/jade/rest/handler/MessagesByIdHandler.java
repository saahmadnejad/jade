package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.service.MessageTrafficService;

/**
 * Handler for {@code GET /api/messages/:id} — returns one captured message
 * with its FULL, untruncated content (list/stream views truncate).
 *
 * <p><b>Old GUI implementation:</b>
 * Double-clicking a message in the Sniffer canvas opened the full ACL view
 * ({@code io.donbee.jade.tools.sniffer.ViewMessage}). This handler is the
 * REST equivalent, backed by {@link MessageTrafficService#getById(String)}.</p>
 */
public class MessagesByIdHandler implements Handler<RoutingContext> {

    private final MessageTrafficService trafficService;

    public MessagesByIdHandler(MessageTrafficService trafficService) {
        this.trafficService = trafficService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            JsonObject message = trafficService.getById(ctx.pathParam("id"));
            if (message == null) {
                ctx.fail(404, new RuntimeException("Message not found (it may have been evicted from the buffer)"));
                return;
            }
            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(message.toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }
}
