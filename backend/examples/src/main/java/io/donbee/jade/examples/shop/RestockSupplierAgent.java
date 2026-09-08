package io.donbee.jade.examples.shop;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.core.behaviours.WakerBehaviour;
import io.donbee.jade.domain.DFService;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;
import io.donbee.jade.proto.AchieveREResponder;

/**
 * Wholesale supplier agent of the online shop.
 *
 * <p>Registers as service type {@code supplier} in the DF. Honours
 * {@code restock <sku> <qty>} requests (FIPA-REQUEST): immediately agrees,
 * then ships after a configurable delay and reports
 * {@code (restocked <sku> <qty>)} back to the requester.</p>
 *
 * <p>Arguments:</p>
 * <ol>
 *   <li>shipping delay in seconds (optional, default 5)</li>
 * </ol>
 */
public class RestockSupplierAgent extends Agent {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(RestockSupplierAgent.class.getName());

    static final String SERVICE_TYPE = "supplier";
    private static final int DEFAULT_SHIPPING_DELAY_SEC = 5;

    private long shippingDelayMs = DEFAULT_SHIPPING_DELAY_SEC * 1000L;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        try {
            shippingDelayMs = Integer.parseInt(args[0].toString().trim()) * 1000L;
        } catch (Exception ignored) {
            // Keep default delay
        }

        try {
            DfUtils.registerService(this, SERVICE_TYPE, "online-shop-supplier");
            LOG.info("registered in DF, ships after "
                + (shippingDelayMs / 1000) + "s");
        } catch (Exception e) {
            LOG.log(io.donbee.jade.util.Logger.SEVERE, "DF registration failed", e);
            doDelete();
            return;
        }

        addBehaviour(new AchieveREResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)) {
            @Override
            protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
                InventoryAgent.RestockOrder order =
                    InventoryAgent.RestockOrder.parse(request.getContent(), "restock");
                ACLMessage reply = request.createReply();
                if (order == null) {
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("(explain expected 'restock <sku> <qty>') ");
                    return reply;
                }

                // Agree now; ship later.
                reply.setPerformative(ACLMessage.AGREE);
                reply.setContent("(accepted " + order.sku() + " " + order.quantity() + ")");

                final AID requester = request.getSender();
                final String shipmentContent = "(restocked " + order.sku() + " " + order.quantity() + ")";
                final String conversationId = "shipment-" + order.sku() + "-" + System.currentTimeMillis();

                myAgent.addBehaviour(new WakerBehaviour(myAgent, shippingDelayMs) {
                    @Override
                    protected void onWake() {
                        ACLMessage shipment = new ACLMessage(ACLMessage.INFORM);
                        shipment.addReceiver(requester);
                        shipment.setProtocol(io.donbee.jade.domain.FIPANames.InteractionProtocol.FIPA_REQUEST);
                        shipment.setConversationId(conversationId);
                        shipment.setContent(shipmentContent);
                        myAgent.send(shipment);
                        LOG.info("shipped " + order.quantity()
                            + " x " + order.sku() + " to " + requester.getLocalName());
                    }
                });
                LOG.info("accepted restock of "
                    + order.quantity() + " x " + order.sku());
                return reply;
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception ignored) {
            // Already gone
        }
    }
}
