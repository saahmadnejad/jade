package io.donbee.jade.core.messaging;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import io.donbee.jade.core.AID;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.util.Logger;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Vert.x EventBus bridge notified by the messaging service about dispatched ACL messages.
 *
 * <p>Phase 2 Step 1 of ADR-0004: Dispatched messages are published to the Vert.x EventBus
 * address {@link #EVENT_BUS_ADDRESS} as JSON objects. This allows headless monitoring
 * consumers (WebSocket stream, recent buffer) to consume live traffic from the shared
 * Vert.x event fabric without blocking agent dispatch threads. Static fan-out has retired.</p>
 *
 * <p><b>Old GUI implementation:</b> replaces the tool-agent registration used
 * by {@code io.donbee.jade.tools.sniffer.Sniffer} (which registered itself via
 * {@code jade.tools.ToolAgent}-style introspection) with direct Vert.x EventBus publication.</p>
 */
public final class MessageTrafficMonitor {

    /** Address on the Vert.x EventBus for full captured ACL message traffic. */
    public static final String EVENT_BUS_ADDRESS = "jade.messages.traffic";

    /** Address on the Vert.x EventBus for truncated live streaming traffic (WebSocket). */
    public static final String EVENT_BUS_STREAM_ADDRESS = "jade.messages.stream";

    private static final Logger logger = Logger.getMyLogger(MessageTrafficMonitor.class.getName());
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static volatile Vertx vertx;

    private MessageTrafficMonitor() {
    }

    /**
     * Initialize the Vert.x instance used for EventBus publication.
     */
    public static void init(Vertx vx) {
        vertx = vx;
    }

    /**
     * Reset the monitor (used in tests and during platform shutdown).
     */
    public static void reset() {
        vertx = null;
        SEQUENCE.set(0);
    }

    /**
     * Notify about a dispatched message by publishing to the Vert.x EventBus.
     * Never throws: monitoring failures must not affect message delivery.
     */
    public static void notifyMessage(AID sender, AID receiver, ACLMessage message) {
        if (sender == null || receiver == null || message == null) {
            return;
        }

        Vertx vx = vertx;
        if (vx == null) {
            return;
        }

        try {
            JsonObject json = toJson(SEQUENCE.incrementAndGet(), sender, receiver, message);
            vx.eventBus().publish(EVENT_BUS_ADDRESS, json);
        } catch (Throwable t) {
            logger.log(Logger.WARNING, "Failed to publish ACL message traffic to EventBus", t);
        }
    }

    /**
     * Serialize an ACL message dispatch event into a {@link JsonObject} matching the
     * schema expected by REST and frontend consumers.
     */
    public static JsonObject toJson(long id, AID sender, AID receiver, ACLMessage message) {
        String content = message.getContent();
        return new JsonObject()
            .put("id", Long.toString(id))
            .put("timestamp", Instant.now().toString())
            .put("sender", localName(sender))
            .put("receiver", localName(receiver))
            .put("performative", ACLMessage.getPerformative(message.getPerformative()).toLowerCase())
            .put("protocol", message.getProtocol() != null ? message.getProtocol() : "")
            .put("ontology", message.getOntology() != null ? message.getOntology() : "")
            .put("content", content != null ? content : "");
    }

    private static String localName(AID aid) {
        if (aid == null) {
            return "";
        }
        String name = aid.getLocalName();
        return name != null ? name : "";
    }
}
