package io.donbee.jade.core.messaging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.donbee.jade.core.AID;
import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Registry of {@link MessageTrafficListener}s notified by the messaging
 * service about dispatched ACL messages. Kept as a small static registry so
 * that both the kernel service and the REST layer can reach it without
 * wiring through the service manager.
 *
 * <p><b>Old GUI implementation:</b> replaces the tool-agent registration used
 * by {@code io.donbee.jade.tools.sniffer.Sniffer} (which registered itself via
 * {@code jade.tools.ToolAgent}-style introspection) with direct in-process
 * notification.</p>
 */
public final class MessageTrafficMonitor {

    private static final List<MessageTrafficListener> LISTENERS = new CopyOnWriteArrayList<>();

    private MessageTrafficMonitor() {
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
     * Notify all listeners about a dispatched message.
     * Never throws: listener failures must not affect message delivery.
     */
    public static void notifyMessage(AID sender, AID receiver, ACLMessage message) {
        if (LISTENERS.isEmpty() || sender == null || receiver == null || message == null) {
            return;
        }
        for (MessageTrafficListener l : LISTENERS) {
            try {
                l.onMessage(sender, receiver, message);
            } catch (Throwable t) {
                // Isolate faulty listeners from the messaging path
            }
        }
    }
}
