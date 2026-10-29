package io.donbee.jade.rest.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import io.donbee.jade.core.Runtime;
import io.donbee.jade.core.messaging.MessageTrafficMonitor;

/**
 * Service that captures platform ACL message traffic for the REST layer.
 * Keeps an in-memory ring buffer of recent messages (served by
 * {@code GET /api/messages/recent} and {@code GET /api/messages/:id}).
 *
 * <p>Phase 2 Step 1 of ADR-0004: Consumes full message events directly from
 * {@link MessageTrafficMonitor#EVENT_BUS_ADDRESS} and buffers them in-memory.
 * Static fan-out and intermediate relay topics have been retired in favor of
 * single-topic EventBus consumption.</p>
 *
 * <p><b>Old GUI implementation:</b> The old Swing Sniffer
 * ({@code io.donbee.jade.tools.sniffer.Sniffer}, see
 * {@code sniffer.MMCanvas} / {@code sniffer.Agent}) displayed live message
 * exchanges by registering as a tool agent receiving introspection events.
 * This service is the headless equivalent: it captures traffic from the Vert.x
 * EventBus and serves the data to the React MessagesPage through the messages API.</p>
 */
public class MessageTrafficService {

    /** Default ring-buffer capacity. */
    public static final int DEFAULT_CAPACITY = 500;

    private static final int CONTENT_TRUNCATE_LENGTH = 256;

    private final int capacity;
    private final Deque<JsonObject> buffer;
    private long dropped = 0;
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
     * Subscribes to {@link MessageTrafficMonitor#EVENT_BUS_ADDRESS} and buffers incoming captures.
     */
    public synchronized void start(Vertx vertx) {
        if (vertx == null) {
            return;
        }
        if (consumer == null) {
            consumer = vertx.eventBus().consumer(MessageTrafficMonitor.EVENT_BUS_ADDRESS, msg -> {
                onMessage(msg.body());
            });
        }
    }

    /**
     * Start capturing using the shared Vert.x instance from Runtime.
     */
    public synchronized void start() {
        Runtime rt = Runtime.instance();
        start(rt.getVertx());
    }

    /**
     * Unregister from the EventBus consumer.
     */
    public synchronized void stop() {
        if (consumer != null) {
            consumer.unregister();
            consumer = null;
        }
    }

    /**
     * Ingest a captured message represented as a {@link JsonObject}.
     * Buffers the message in the ring buffer.
     */
    public void onMessage(JsonObject json) {
        if (json == null) {
            return;
        }

        synchronized (buffer) {
            if (buffer.size() >= capacity) {
                buffer.removeFirst();
                dropped++;
            }
            buffer.addLast(json.copy());
        }
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

    /** Copy with truncated content, for table/list rendering. */
    public static JsonObject truncated(JsonObject full) {
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
