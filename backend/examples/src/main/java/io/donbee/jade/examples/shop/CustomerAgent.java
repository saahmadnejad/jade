package io.donbee.jade.examples.shop;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.core.behaviours.TickerBehaviour;
import io.donbee.jade.domain.FIPANames;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;

import java.util.Random;

/**
 * Simulated customer of the online shop.
 *
 * <p>Every {@code tickSeconds} discovers the shop via DF search and sends a
 * purchase request {@code (buy <sku> <qty>)} for a random SKU. Incoming
 * outcomes are logged.</p>
 *
 * <p>Arguments (comma-separated in the agent specifier):</p>
 * <ol>
 *   <li>purchase interval in seconds (optional, default 8)</li>
 *   <li>quantity per purchase (optional, default 1)</li>
 *   <li>one or more SKU names (optional, default "sku-1" and "sku-2")</li>
 * </ol>
 */
public class CustomerAgent extends Agent {

    private static final int DEFAULT_INTERVAL_SEC = 8;
    private static final int DEFAULT_QUANTITY = 1;

    private String[] skus = {"sku-1", "sku-2"};
    private int quantity = DEFAULT_QUANTITY;
    private int purchaseCounter = 0;
    private final Random random = new Random();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        int intervalSec = DEFAULT_INTERVAL_SEC;
        if (args.length > 0) {
            try {
                intervalSec = Integer.parseInt(args[0].toString().trim());
            } catch (NumberFormatException ignored) {
                // Keep default
            }
        }
        if (args.length > 1) {
            try {
                quantity = Math.max(1, Integer.parseInt(args[1].toString().trim()));
            } catch (NumberFormatException ignored) {
                // Keep default
            }
        }
        if (args.length > 2) {
            String[] configured = new String[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                configured[i - 2] = args[i].toString().trim();
            }
            skus = configured;
        }

        addBehaviour(new TickerBehaviour(this, intervalSec * 1000L) {
            @Override
            protected void onTick() {
                try {
                    purchaseOnce();
                } catch (Exception e) {
                    System.err.println("[CustomerAgent] purchase attempt failed: " + e);
                }
            }
        });

        // Log outcomes coming back from the shop.
        MessageTemplate outcomes = MessageTemplate.or(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                MessageTemplate.MatchPerformative(ACLMessage.FAILURE)));
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage outcome = myAgent.receive(outcomes);
                if (outcome == null) {
                    block();
                    return;
                }
                switch (outcome.getPerformative()) {
                    case ACLMessage.INFORM ->
                        System.out.println("[CustomerAgent] happy: " + outcome.getContent());
                    case ACLMessage.REFUSE ->
                        System.out.println("[CustomerAgent] disappointed: " + outcome.getContent());
                    default ->
                        System.out.println("[CustomerAgent] error: " + outcome.getContent());
                }
            }
        });
    }

    private void purchaseOnce() throws Exception {
        AID shop = DfUtils.findServiceProvider(this, ShopAgent.SERVICE_TYPE);
        if (shop == null) {
            System.out.println("[CustomerAgent] no shop found in DF yet");
            return;
        }
        String sku = skus[random.nextInt(skus.length)].trim();
        purchaseCounter++;
        ACLMessage buy = new ACLMessage(ACLMessage.REQUEST);
        buy.addReceiver(shop);
        buy.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        buy.setConversationId("purchase-" + getLocalName() + "-" + purchaseCounter);
        buy.setContent("(buy " + sku + " " + quantity + ")");
        send(buy);
        System.out.println("[CustomerAgent] wants " + quantity + " x " + sku);
    }
}
