package io.donbee.jade.rest.service;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.core.AgentManager;
import io.donbee.jade.core.ContainerID;
import io.donbee.jade.domain.FIPANames;
import io.donbee.jade.domain.FIPAAgentManagement.FIPAManagementOntology;
import io.donbee.jade.domain.DFGUIManagement.DFAppletOntology;
import io.donbee.jade.content.ContentElement;
import io.donbee.jade.content.ContentManager;
import io.donbee.jade.content.lang.sl.SLCodec;
import io.donbee.jade.content.onto.Ontology;
import io.donbee.jade.content.onto.basic.Action;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary one-shot agent that sends an ACL REQUEST to the DF agent
 * and blocks until the DF responds. Used by the REST API to perform
 * synchronous DF operations (register, deregister, modify, search)
 * from outside an agent context.
 */
public class DFRequestAgent extends Agent {

    private static final Map<String, DFRequestAgent.ResultHolder> results = new ConcurrentHashMap<>();

    private static class ResultHolder {
        ACLMessage response;
        ContentElement decoded;
        RuntimeException error;
        boolean done = false;
    }

    /**
     * Outcome of a synchronous DF request: the raw reply message and,
     * when available, the decoded content (e.g. a {@code Result} for queries).
     */
    public static final class DFResponse {
        public final ACLMessage message;
        public final ContentElement decoded;

        DFResponse(ACLMessage message, ContentElement decoded) {
            this.message = message;
            this.decoded = decoded;
        }
    }

    /**
     * Submit a synchronous DF request. The caller blocks until the agent
     * completes the operation or the timeout expires.
     *
     * @param agentManager  the AMS (for creating and killing the temp agent)
     * @param containerID   the container to create the agent in
     * @param receiverAID   the AID of the DF agent
     * @param action        the FIPA management action concept
     * @param ontologyName  the ontology name for encoding
     * @param resultCode    the FIPAManagementVocabulary action code (unused, reserved for diagnostics)
     * @param timeoutMs     timeout in milliseconds for the DF response
     * @return the raw ACLMessage response from the DF (may be null if timed out)
     */
    public static ACLMessage execute(AgentManager agentManager,
                                     ContainerID containerID,
                                     AID receiverAID,
                                     io.donbee.jade.content.Concept action,
                                     String ontologyName,
                                     String resultCode,
                                     long timeoutMs) {
        return executeFull(agentManager, containerID, receiverAID, action, ontologyName, timeoutMs).message;
    }

    /**
     * Submit a synchronous DF request and return both the raw reply message
     * and, when possible, the decoded content element.
     */
    public static DFResponse executeFull(AgentManager agentManager,
                                         ContainerID containerID,
                                         AID receiverAID,
                                         io.donbee.jade.content.Concept action,
                                         String ontologyName,
                                         long timeoutMs) {
        String key = "df-request-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
        ResultHolder holder = new ResultHolder();
        results.put(key, holder);

        String agentName = "df_req_" + System.currentTimeMillis();
        Object[] args = { key, receiverAID, action, ontologyName, timeoutMs };

        try {
            agentManager.create(agentName, DFRequestAgent.class.getName(), args, containerID, null, null, null, null);
        } catch (Exception e) {
            results.remove(key);
            throw new RuntimeException("Failed to create DF request agent: " + e.getMessage(), e);
        }

        // Wait for the agent to complete
        long deadline = System.currentTimeMillis() + timeoutMs + 10000;
        while (!holder.done && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        results.remove(key);
        if (holder.error != null) {
            throw holder.error;
        }
        return new DFResponse(holder.response, holder.decoded);
    }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args == null || args.length < 5) {
            doDelete();
            return;
        }

        String key = (String) args[0];
        AID receiverAID = (AID) args[1];
        io.donbee.jade.content.Concept action = (io.donbee.jade.content.Concept) args[2];
        String ontologyName = (String) args[3];
        long timeoutMs = (Long) args[4];

        ResultHolder holder = results.get(key);
        if (holder == null) {
            doDelete();
            return;
        }

        try {
            ContentManager cm = getContentManager();
            // SLCodec() (no-arg) registers only under "fipa-sl"; explicitly
            // register it under fipa-sl0, which is the language used for both
            // the FIPA management and DF-Applet requests. The same full-SL
            // codec parses all SL variants.
            SLCodec slCodec = new SLCodec();
            cm.registerLanguage(slCodec, FIPANames.ContentLanguage.FIPA_SL0);

            if (ontologyName.equals(FIPAManagementOntology.getInstance().getName())) {
                cm.registerOntology(FIPAManagementOntology.getInstance());
            } else if (ontologyName.equals(DFAppletOntology.getInstance().getName())) {
                cm.registerOntology(DFAppletOntology.getInstance());
            }

            Action act = new Action();
            // The "actor" slot is mandatory for FIPA-SL encoding. The actor is the
            // agent requesting the action — this temporary agent, which IS locally
            // registered (so the DF can resolve and reply to it).
            act.setActor(getAID());
            act.setAction(action);

            ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
            requestMsg.addReceiver(receiverAID);
            requestMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
            requestMsg.setOntology(ontologyName);
            cm.fillContent(requestMsg, act);

            send(requestMsg);

            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.FAILURE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                )
            );

            ACLMessage reply = blockingReceive(mt, (int) timeoutMs);
            holder.response = reply;
            // Best-effort decode of the reply content so callers can read
            // structured results (e.g. the parent AID list of a GetParents reply).
            if (reply != null && reply.getPerformative() == ACLMessage.INFORM) {
                try {
                    holder.decoded = cm.extractContent(reply);
                } catch (Exception e) {
                    // Decoding is best-effort; the raw message is still available.
                }
            }
        } catch (RuntimeException e) {
            holder.error = e;
        } catch (Exception e) {
            holder.error = new RuntimeException(e);
        } finally {
            holder.done = true;
            doDelete();
        }
    }
}
