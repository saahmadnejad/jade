package io.donbee.jade.rest.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.messaging.MessageTrafficListener;
import io.donbee.jade.core.messaging.MessageTrafficMonitor;
import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Service that captures platform ACL message traffic for the REST layer.
 * Keeps an in-memory ring buffer of recent messages (served by
 * {@code GET /api/messages/recent}) and forwards each capture to subscribed
 * consumers (the WebSocket stream handler).
 *
 * <p>Phase 2 Step 1 of ADR-0004: In addition to the legacy listener interface,
 * this service registers as a consumer on the Vert.x EventBus
 * ({@link MessageTrafficMonitor#EVENT_BUS_ADDRESS}) when started with a {@link Vertx}
 * instance.</p>
 *
 * <p><b>Old GUI implementation:</b> The old Swing Sniffer
 * ({@code io.donbee.jade.tools.sniffer.Sniffer}, see
 * {@code sniffer.MMCanvas} / {@code sniffer.Agent}) displayed live message
 * exchanges by registering as a tool agent receiving introspection events.
 * This service is the headless equivalent: it captures traffic from the Vert.x
 * EventBus (or via {@link MessageTrafficListener}) and serves the data to the
 * React MessagesPage through the messages API.</p>
 */
public class MessageTrafficService implements MessageTrafficListener {

    /** Default ring-buffer capacity. */
    public static final int DEFAULT_CAPACITY = 500;

    private static final int CONTENT_TRUNCATE_LENGTH = 256;

    private final int capacity;
    private final Deque<JsonObject> buffer;
    private long dropped = 0;
    private long sequence = 0;
    private final List<Consumer<JsonObject>> subscribers = new ArrayList<>();
    private MessageConsumer<JsonObject> consumer;

    public MessageTrafficService() {
        this(DEFAULT_CAPACITY);
    }

    public MessageTrafficService(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.buffer = new ArrayDeque<>(this.capacity);
    }

    /**
     * Start capturing message traffic via Vert.x EventBus.
     * When vertx is non-null, subscribes to {@link MessageTrafficMonitor#EVENT_BUS_ADDRESS}.
     * When vertx is null, falls back to {@link MessageTrafficMonitor#addListener}.
     */
    public synchronized void start(Vertx vertx) {
        if (vertx != null) {
            if (consumer == null) {
                consumer = vertx.eventBus().consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> {
                    onMessage(msg.body());
                });
            }
        } else {
            MessageTrafficMonitor.addListener(this);
        }
    }

    /**
     * Register this service as a platform traffic listener.
     * Starts using the shared Vert.x instance if active, otherwise legacy fallback.
     */
    public synchronized void start() {
        io.donbee.jade.core.Runtime rt = io.donbee.jade.core.Runtime.instance();
        start(rt != null && rt.hasVertx() ? rt.getVertx() : null);
    }

    /**
     * Unregister from the EventBus consumer and legacy platform traffic listeners.
     */
    public synchronized void stop() {
        if (consumer != null) {
            consumer.unregister();
            consumer = null;
        }
        MessageTrafficMonitor.removeListener(this);
    }

    @Override
    public void onMessage(AID sender, AID receiver, ACLMessage message) {
        onMessage(MessageTrafficMonitor.toJson(++sequence, sender, receiver, message));
    }

    /**
     * Ingest a captured message represented as a {@link JsonObject}.
     * Assigns a local sequence ID if missing, buffers the message in the ring buffer,
     * and pushes a truncated copy to active subscribers.
     */
    public void onMessage(JsonObject json) {
        if (json == null) {
            return;
        }
        if (!json.containsKey("id") || json.getString("id", "").isBlank()) {
            json.put("id", Long.toString(++sequence));
        }

        synchronized (buffer) {
            if (buffer.size() >= capacity) {
                buffer.removeFirst();
                dropped++;
            }
            buffer.addLast(json.copy());
        }
        notifySubscribers(truncated(json));
    }

    /**
     * Full single message by its capture id, with untruncated content.
     *
     * @return the message, or null when no capture with this id exists
     */
    public JsonObject getById(String id) {
        synchronized (buffer) {
            for (JsonObject msg : buffer) {
                if (msg.getString("id", "").equals(id)) {
                    return msg;
                }
            }
        }
        return null;
    }

    /**
     * Snapshot of the buffered messages, oldest first, with optional filters
     * and truncated content (for table rendering).
     */
    public List<JsonObject> recent(int limit, String from, String to) {
        int max = limit > 0 ? limit : 100;
        List<JsonObject> result = new ArrayList<>();
        synchronized (buffer) {
            for (JsonObject msg : buffer) {
                if (matches(msg, from, to)) {
                    result.add(truncated(msg));
                }
            }
        }
        int size = result.size();
        return size <= max ? result : new ArrayList<>(result.subList(size - max, size));
    }

    public long getDroppedCount() {
        synchronized (buffer) {
            return dropped;
        }
    }

    /**
     * Subscribe a consumer to live captures. Returns an unsubscribe handle.
     */
    public Runnable subscribe(Consumer<JsonObject> consumer) {
        synchronized (subscribers) {
            subscribers.add(consumer);
        }
        return () -> {
            synchronized (subscribers) {
                subscribers.remove(consumer);
            }
        };
    }

    private void notifySubscribers(JsonObject json) {
        List<Consumer<JsonObject>> current;
        synchronized (subscribers) {
            current = new ArrayList<>(subscribers);
        }
        for (Consumer<JsonObject> c : current) {
            try {
                c.accept(json);
            } catch (Exception e) {
                // Isolate faulty subscribers
            }
        }
    }

    /** Copy with truncated content, for table/list rendering. */
    static JsonObject truncated(JsonObject full) {
        String content = full.getString("content", "");
        if (content.length() <= CONTENT_TRUNCATE_LENGTH) {
            return full;
        }
        JsonObject copy = full.copy();
        copy.put("content", content.substring(0, CONTENT_TRUNCATE_LENGTH) + "...");
        return copy;
    }

    private static boolean matches(JsonObject msg, String from, String to) {
        if (from != null && !msg.getString("sender", "").contains(from)) {
            return false;
        }
        if (to != null && !msg.getString("receiver", "").contains(to)) {
            return false;
        }
        return true;
    }
}
