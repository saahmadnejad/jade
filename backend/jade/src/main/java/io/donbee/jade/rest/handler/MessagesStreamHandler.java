package io.donbee.jade.rest.handler;

import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import io.donbee.jade.core.messaging.MessageTrafficMonitor;
import io.donbee.jade.rest.service.MessageTrafficService;

/**
 * Handler for the live message stream ({@code WS /api/messages/stream}).
 * Upgrades the HTTP request to a WebSocket and streams newly captured
 * ACL messages directly from the Vert.x EventBus as text frames.
 *
 * <p>Phase 2 Step 1 of ADR-0004: Subscribes directly to the Vert.x EventBus
 * address {@link MessageTrafficMonitor#EVENT_BUS_ADDRESS}. Writes are
 * dispatched to the WebSocket connection's event-loop context with backpressure
 * protection via consumer pause and drain handler, and consumers are unregistered
 * on close and error.</p>
 *
 * <p><b>Old GUI implementation:</b>
 * No direct Swing GUI equivalent: the old tools were fat clients observing
 * messages in-process ({@code io.donbee.jade.tools.sniffer.Sniffer} via
 * tool-agent introspection events). This endpoint exposes the same live view
 * over a WebSocket so the React MessagesPage can render it. It delegates capture
 * delivery directly to the Vert.x EventBus topic {@link MessageTrafficMonitor#EVENT_BUS_ADDRESS}.</p>
 */
public class MessagesStreamHandler implements Handler<RoutingContext> {

    public MessagesStreamHandler() {
    }

    /**
     * @deprecated MessageTrafficService is no longer needed; this handler subscribes directly to the EventBus.
     */
    @Deprecated
    public MessagesStreamHandler(MessageTrafficService trafficService) {
        this();
    }

    @Override
    public void handle(RoutingContext ctx) {
        ctx.request().toWebSocket().onComplete(ws -> {
            if (ws.failed()) {
                ctx.fail(ws.cause());
                return;
            }
            var socket = ws.result();
            Context wsContext = Vertx.currentContext();
            AtomicReference<MessageConsumer<JsonObject>> consumerRef = new AtomicReference<>();

            MessageConsumer<JsonObject> consumer = ctx.vertx().eventBus().consumer(
                MessageTrafficMonitor.EVENT_BUS_ADDRESS,
                msg -> {
                    Runnable writeTask = () -> {
                        if (socket.isClosed()) {
                            return;
                        }
                        JsonObject payload = MessageTrafficService.truncated(msg.body());
                        socket.writeTextMessage(payload.encode());
                        if (socket.writeQueueFull()) {
                            MessageConsumer<JsonObject> c = consumerRef.get();
                            if (c != null) {
                                c.pause();
                                socket.drainHandler(v -> c.resume());
                            }
                        }
                    };
                    if (wsContext != null) {
                        wsContext.runOnContext(v -> writeTask.run());
                    } else {
                        writeTask.run();
                    }
                }
            );

            consumerRef.set(consumer);
            socket.closeHandler(v -> consumer.unregister());
            socket.exceptionHandler(err -> consumer.unregister());
        });
    }
}
