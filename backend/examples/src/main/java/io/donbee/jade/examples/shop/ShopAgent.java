package io.donbee.jade.examples.shop;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.domain.DFService;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;

import java.util.logging.Level;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storefront agent of the online shop.
 *
 * <p>Registers as service type {@code shop} in the DF. Receives purchase
 * requests ({@code buy <sku> <qty>}) from customers, asks the inventory agent
 * to reserve the stock and reports the outcome back. The inventory round-trip
 * is correlated via conversation ids.</p>
 *
 * <p>Content language: plain text, e.g. {@code (buy sku-1 2)}.</p>
 */
public class ShopAgent extends Agent {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(ShopAgent.class.getName());

    static final String SERVICE_TYPE = "shop";

    private final Map<String, PendingPurchase> pendingPurchases = new ConcurrentHashMap<>();

    private record PendingPurchase(AID customer, String sku, int quantity) {
    }

    @Override
    protected void setup() {
        try {
            DfUtils.registerService(this, SERVICE_TYPE, "online-shop-storefront");
            LOG.info("registered in DF, ready to take orders");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "DF registration failed", e);
            doDelete();
            return;
        }

        MessageTemplate requests = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        // 1) Accept purchase requests from customers.
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage request = myAgent.receive(requests);
                if (request == null) {
                    block();
                    return;
                }
                Purchase purchase = Purchase.parse(request.getContent(), "buy");
                if (purchase == null) {
                    reply(request, ACLMessage.NOT_UNDERSTOOD, "(explain expected 'buy <sku> <qty>')");
                    return;
                }

                AID inventory = findInventory();
                if (inventory == null) {
                    reply(request, ACLMessage.FAILURE, "(error no-inventory-agent)");
                    return;
                }

                String reserveConvId = request.getConversationId() + "-reserve";
                pendingPurchases.put(reserveConvId,
                    new PendingPurchase(request.getSender(), purchase.sku(), purchase.quantity()));

                ACLMessage reserve = new ACLMessage(ACLMessage.REQUEST);
                reserve.addReceiver(inventory);
                reserve.setProtocol(io.donbee.jade.domain.FIPANames.InteractionProtocol.FIPA_REQUEST);
                reserve.setConversationId(reserveConvId);
                reserve.setContent("(reserve " + purchase.sku() + " " + purchase.quantity() + ")");
                myAgent.send(reserve);
            }
        });

        // 2) Handle the inventory's reservation outcome and answer the customer.
        MessageTemplate reserveReplies = MessageTemplate.and(
            new MessageTemplate((msg) -> {
                String convId = msg.getConversationId();
                return convId != null && convId.endsWith("-reserve");
            }),
            MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchPerformative(ACLMessage.REFUSE)));
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage response = myAgent.receive(reserveReplies);
                if (response == null) {
                    block();
                    return;
                }
                PendingPurchase pending = pendingPurchases.remove(response.getConversationId());
                if (pending == null) {
                    return;
                }
                if (response.getPerformative() == ACLMessage.INFORM) {
                    reply(response, ACLMessage.INFORM,
                        "(order-confirmed " + pending.sku() + " " + pending.quantity() + ")");
                    LOG.info("order confirmed for "
                        + pending.customer().getLocalName() + ": " + pending.sku());
                } else {
                    reply(response, ACLMessage.REFUSE, "(out-of-stock " + pending.sku() + ")");
                    LOG.info("refused order for "
                        + pending.customer().getLocalName() + ": out of stock " + pending.sku());
                }
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

    private AID findInventory() {
        try {
            return DfUtils.findServiceProvider(this, InventoryAgent.SERVICE_TYPE);
        } catch (Exception e) {
            LOG.warning("DF search failed: " + e);
            return null;
        }
    }

    private void reply(ACLMessage original, int performative, String content) {
        ACLMessage reply = original.createReply();
        reply.setPerformative(performative);
        reply.setContent(content);
        send(reply);
    }

    record Purchase(String sku, int quantity) {
        /** Parse {@code "<verb> <sku> <quantity>"} with or without parentheses. */
        static Purchase parse(String content, String verb) {
            if (content == null) {
                return null;
            }
            String cleaned = content.replace("(", " ").replace(")", " ").trim();
            String[] tokens = cleaned.split("\\s+");
            if (tokens.length != 3 || !tokens[0].equals(verb)) {
                return null;
            }
            try {
                int qty = Integer.parseInt(tokens[2]);
                return qty > 0 ? new Purchase(tokens[1], qty) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
