package io.donbee.jade.rest.handler;

import java.util.function.Consumer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.rest.service.MessageTrafficService;

/**
 * Handler for the live message stream ({@code WS /api/messages/stream}).
 * Upgrades the HTTP request to a WebSocket and pushes every newly captured
 * ACL message as a single JSON frame.
 *
 * <p><b>Old GUI implementation:</b>
 * No direct Swing GUI equivalent: the old tools were fat clients observing
 * messages in-process ({@code io.donbee.jade.tools.sniffer.Sniffer} via
 * tool-agent introspection events). This endpoint exposes the same live view
 * over a WebSocket so the React MessagesPage can render it. It delegates to
 * {@link MessageTrafficService#subscribe(Consumer)} for capture delivery.</p>
 */
public class MessagesStreamHandler implements Handler<RoutingContext> {

    private final MessageTrafficService trafficService;

    public MessagesStreamHandler(MessageTrafficService trafficService) {
        this.trafficService = trafficService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        ctx.request().toWebSocket().onComplete(ws -> {
            if (ws.failed()) {
                ctx.fail(ws.cause());
                return;
            }
            var socket = ws.result();

            Consumer<io.vertx.core.json.JsonObject> subscriber = msg ->
                socket.writeTextMessage(msg.encode());

            Runnable unsubscribe = trafficService.subscribe(subscriber);

            socket.closeHandler(v -> unsubscribe.run());
        });
    }
}
