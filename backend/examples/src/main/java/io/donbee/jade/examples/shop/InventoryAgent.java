package io.donbee.jade.examples.shop;

import java.util.Map;
import java.util.Random;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.core.behaviours.TickerBehaviour;
import io.donbee.jade.domain.DFService;
import io.donbee.jade.domain.FIPANames;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;
import io.donbee.jade.proto.AchieveREResponder;

/**
 * Warehouse agent of the online shop.
 *
 * <p>Registers as service type {@code inventory} in the DF. Honours
 * {@code reserve <sku> <qty>} requests (FIPA-REQUEST) from the shop,
 * periodically checks for SKUs at or below the restock threshold and asks a
 * supplier found via the DF to replenish them. When the supplier reports
 * {@code (restocked <sku> <qty>)} the stock is increased again.</p>
 *
 * <p>Arguments (comma-separated in the agent specifier):</p>
 * <ol>
 *   <li>initial stock config, entries joined with {@code |}: {@code sku-1:10|sku-2:5} (optional)</li>
 *   <li>restock threshold (optional, default 3)</li>
 *   <li>restock check interval in seconds (optional, default 10)</li>
 *   <li>restock quantity per order (optional, default 20)</li>
 * </ol>
 */
public class InventoryAgent extends Agent {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(InventoryAgent.class.getName());

    static final String SERVICE_TYPE = "inventory";
    private static final int DEFAULT_THRESHOLD = 3;
    private static final int DEFAULT_CHECK_INTERVAL_SEC = 10;
    private static final int DEFAULT_RESTOCK_QUANTITY = 20;

    private Inventory inventory;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        String stockConfig = args.length > 0 ? args[0].toString() : null;
        int threshold = intArg(args, 1, DEFAULT_THRESHOLD);
        int checkIntervalSec = intArg(args, 2, DEFAULT_CHECK_INTERVAL_SEC);
        int restockQuantity = intArg(args, 3, DEFAULT_RESTOCK_QUANTITY);

        inventory = Inventory.fromConfig(stockConfig, threshold);

        try {
            DfUtils.registerService(this, SERVICE_TYPE, "online-shop-inventory");
            LOG.info("registered in DF with stock: " + describeStock());
        } catch (Exception e) {
            LOG.warning("DF registration failed: " + e);
            doDelete();
            return;
        }

        // Reservation requests coming from the shop.
        addBehaviour(new AchieveREResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)) {
            @Override
            protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
                Reservation reservation = Reservation.parse(request.getContent());
                ACLMessage reply = request.createReply();
                if (reservation != null && inventory.reserve(reservation.sku(), reservation.quantity())) {
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("(reserved " + reservation.sku() + " " + reservation.quantity() + ")");
                    LOG.info("reserved " + reservation.quantity()
                        + " x " + reservation.sku() + " (stock now " + inventory.getStock(reservation.sku()) + ")");
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("(insufficient-stock)");
                    LOG.info("refused reservation: " + request.getContent());
                }
                return reply;
            }
        });

        // Shipment confirmations coming back from suppliers.
        MessageTemplate shipmentConfirmations = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            new MessageTemplate((msg) -> {
                String content = msg.getContent();
                return content != null && content.contains("restocked");
            }));
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage confirmation = myAgent.receive(shipmentConfirmations);
                if (confirmation == null) {
                    block();
                    return;
                }
                RestockOrder order = RestockOrder.parse(confirmation.getContent(), "restocked");
                if (order != null) {
                    inventory.addStock(order.sku(), order.quantity());
                    LOG.info("received restock: +" + order.quantity()
                        + " x " + order.sku() + " (stock now " + inventory.getStock(order.sku()) + ")");
                }
            }
        });

        // Periodic low-stock check -> ask supplier for replenishment.
        addBehaviour(new TickerBehaviour(this, checkIntervalSec * 1000L) {
            @Override
            protected void onTick() {
                for (String sku : inventory.lowStockSkus()) {
                    AID supplier = findSupplier();
                    if (supplier == null) {
                        LOG.info("no supplier available for " + sku);
                        continue;
                    }
                    ACLMessage restock = new ACLMessage(ACLMessage.REQUEST);
                    restock.addReceiver(supplier);
                    restock.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    restock.setConversationId("restock-" + sku + "-" + System.currentTimeMillis());
                    restock.setContent("(restock " + sku + " " + restockQuantity + ")");
                    myAgent.send(restock);
                    LOG.info("ordered restock of " + restockQuantity + " x " + sku);
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

    private AID findSupplier() {
        try {
            return DfUtils.findServiceProvider(this, RestockSupplierAgent.SERVICE_TYPE);
        } catch (Exception e) {
            LOG.warning("DF search failed: " + e);
            return null;
        }
    }

    private String describeStock() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : inventory.snapshot().entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return sb.toString();
    }

    private static int intArg(Object[] args, int index, int defaultValue) {
        try {
            return Integer.parseInt(args[index].toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    record Reservation(String sku, int quantity) {
        static Reservation parse(String content) {
            ShopAgent.Purchase purchase = ShopAgent.Purchase.parse(content, "reserve");
            return purchase != null ? new Reservation(purchase.sku(), purchase.quantity()) : null;
        }
    }

    record RestockOrder(String sku, int quantity) {
        static RestockOrder parse(String content, String verb) {
            ShopAgent.Purchase purchase = ShopAgent.Purchase.parse(content, verb);
            return purchase != null ? new RestockOrder(purchase.sku(), purchase.quantity()) : null;
        }
    }
}
