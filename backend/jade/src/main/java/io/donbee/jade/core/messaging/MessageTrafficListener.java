package io.donbee.jade.core.messaging;

import io.donbee.jade.core.AID;
import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Listener notified about ACL messages dispatched by the messaging service.
 * Used to expose live message traffic (e.g. to the REST/WebSocket layer).
 *
 * <p><b>Old GUI implementation:</b> The old Swing Sniffer
 * ({@code io.donbee.jade.tools.sniffer.Sniffer}) and Introspector observed
 * message exchanges by registering as platform tool agents receiving
 * introspection events. This listener provides a lightweight in-process
 * equivalent for headless consumers.</p>
 */
public interface MessageTrafficListener {

    /**
     * Called for each ACL message dispatched from an agent hosted on this container.
     *
     * @param sender   the AID of the sending agent
     * @param receiver the AID of the intended receiver of this dispatch
     * @param message  the ACL message being dispatched
     */
    void onMessage(AID sender, AID receiver, ACLMessage message);
}
