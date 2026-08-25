package io.donbee.jade.rest.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.service.MessageTrafficService;

/**
 * Handler for the recent-messages endpoint ({@code GET /api/messages/recent}).
 * Returns the buffered history of captured ACL message traffic, oldest first.
 *
 * <p><b>Old GUI implementation:</b>
 * The live message view was provided by the Sniffer tool
 * ({@code io.donbee.jade.tools.sniffer.Sniffer}, rendering exchanges on
 * {@code sniffer.MMCanvas}) and the Introspector's message list
 * ({@code io.donbee.jade.tools.introspector.gui.MessagePanel}). Both used the
 * FIPA ACL / introspection ontology via tool-agent registration. This handler
 * instead reads from {@link MessageTrafficService#recent(int, String, String)},
 * which captures messages directly at the messaging-service dispatch point.</p>
 */
public class MessagesRecentHandler implements Handler<RoutingContext> {

    private static final int DEFAULT_LIMIT = 100;

    private final MessageTrafficService trafficService;

    public MessagesRecentHandler(MessageTrafficService trafficService) {
        this.trafficService = trafficService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            Integer limit = parseLimit(ctx.queryParams().get("limit"));
            if (limit == null) {
                ctx.fail(400, new RuntimeException("Invalid 'limit' parameter"));
                return;
            }
            var messages = trafficService.recent(
                limit,
                ctx.queryParams().get("from"),
                ctx.queryParams().get("to"));

            JsonArray arr = new JsonArray();
            messages.forEach(arr::add);

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("messages", arr)
                    .put("total", arr.size())
                    .put("dropped", trafficService.getDroppedCount())
                    .toBuffer());
        } catch (Exception e) {
            ctx.fail(500, e);
        }
    }

    private Integer parseLimit(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LIMIT;
        }
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
