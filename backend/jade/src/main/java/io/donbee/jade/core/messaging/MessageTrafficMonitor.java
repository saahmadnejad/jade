package io.donbee.jade.core.messaging;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Runtime;
import io.donbee.jade.lang.acl.ACLMessage;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Registry of {@link MessageTrafficListener}s and Vert.x EventBus bridge
 * notified by the messaging service about dispatched ACL messages.
 * Kept as a small static registry so that both the kernel service and
 * the REST layer can reach it without wiring through the service manager.
 *
 * <p>Phase 2 Step 1 of ADR-0004: In addition to legacy in-process listeners,
 * dispatched messages are published to the Vert.x EventBus address
 * {@link #EVENT_BUS_ADDRESS} as JSON objects. This allows headless monitoring
 * consumers (WebSocket stream, recent buffer) to consume live traffic from
 * the shared Vert.x event fabric without blocking agent dispatch threads.</p>
 *
 * <p><b>Old GUI implementation:</b> replaces the tool-agent registration used
 * by {@code io.donbee.jade.tools.sniffer.Sniffer} (which registered itself via
 * {@code jade.tools.ToolAgent}-style introspection) with direct in-process
 * notification and Vert.x EventBus publication.</p>
 */
public final class MessageTrafficMonitor {

    /** Address on the Vert.x EventBus for published ACL message traffic. */
    public static final String EVENT_BUS_ADDRESS = "jade.messages.traffic";

    private static final List<MessageTrafficListener> LISTENERS = new CopyOnWriteArrayList<>();
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

    public static void addListener(MessageTrafficListener l) {
        if (l != null && !LISTENERS.contains(l)) {
            LISTENERS.add(l);
        }
    }

    public static void removeListener(MessageTrafficListener l) {
        LISTENERS.remove(l);
    }

    /**
     * Notify all listeners and publish to the Vert.x EventBus about a dispatched message.
     * Never throws: listener and EventBus failures must not affect message delivery.
     */
    public static void notifyMessage(AID sender, AID receiver, ACLMessage message) {
        if (sender == null || receiver == null || message == null) {
            return;
        }

        // 1. Publish to Vert.x EventBus when active
        Vertx vx = getEventBusVertx();
        if (vx != null) {
            try {
                JsonObject json = toJson(SEQUENCE.incrementAndGet(), sender, receiver, message);
                vx.eventBus().publish(EVENT_BUS_ADDRESS, json);
            } catch (Throwable t) {
                // Isolate event bus failures from the messaging path
            }
        }

        // 2. Legacy in-process listener fan-out (backward compatibility during bridge)
        if (!LISTENERS.isEmpty()) {
            for (MessageTrafficListener l : LISTENERS) {
                try {
                    l.onMessage(sender, receiver, message);
                } catch (Throwable t) {
                    // Isolate faulty listeners from the messaging path
                }
            }
        }
    }

    private static Vertx getEventBusVertx() {
        if (vertx == null) {
            Runtime rt = Runtime.instance();
            if (rt != null && rt.hasVertx()) {
                vertx = rt.getVertx();
            }
        }
        return vertx;
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
