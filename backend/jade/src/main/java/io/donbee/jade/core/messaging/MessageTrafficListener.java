package io.donbee.jade.core.messaging;

import io.donbee.jade.core.AID;
import io.donbee.jade.lang.acl.ACLMessage;

/**
 * Listener notified about ACL messages dispatched by the messaging service.
 *
 * @deprecated As of ADR-0004 Phase 2 Step 1, static fan-out has retired in favor of the
 *             Vert.x EventBus address {@link MessageTrafficMonitor#EVENT_BUS_ADDRESS}.
 */
@Deprecated(forRemoval = true)
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
