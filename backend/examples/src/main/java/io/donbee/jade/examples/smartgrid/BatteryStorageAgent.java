package io.donbee.jade.examples.smartgrid;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.donbee.jade.core.Agent;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.core.behaviours.TickerBehaviour;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;

/**
 * Battery Energy Storage System (BESS) agent providing rapid dispatchable reserves
 * during grid generation deficits, and charging when electricity prices are low or negative.
 */
public class BatteryStorageAgent extends Agent {

    private static final Logger LOG = io.donbee.jade.util.Logger.getJADELogger(BatteryStorageAgent.class.getName());

    private SmartGridOntology.BatteryConfig config;
    private double storedKwh = 35.0; // 70% initial state of charge
    private double totalKwhDischarged = 0.0;
    private double totalKwhCharged = 0.0;
    private double lastSeenGridFrequency = 50.0;
    private double lastSeenMarketPrice = 0.12;

    public record BatteryState(double soc, double storedKwh, double totalKwhCharged, double totalKwhDischarged) {}
    private static final Map<String, BatteryState> STATE_REGISTRY = new java.util.concurrent.ConcurrentHashMap<>();

    public static BatteryState getState(String localName) {
        return STATE_REGISTRY.get(localName);
    }

    public static void clearStates() {
        STATE_REGISTRY.clear();
    }

    private void recordState() {
        STATE_REGISTRY.put(getLocalName(), new BatteryState(getSoC(), storedKwh, totalKwhCharged, totalKwhDischarged));
    }

    private PriceAwareChargingTicker chargingTicker;
    private BatteryAuctionResponder auctionResponder;
    private int stepCount = 0;

    @Override
    protected void setup() {
        config = SmartGridOntology.BatteryConfig.fromArgs(getArguments());
        storedKwh = config.capacityKwh() * 0.70;
        recordState();

        if (config.registerWithDf()) {
            SmartGridOntology.registerWithDF(this, SmartGridOntology.DF_SERVICE_PROVIDER, config.gridId(), SmartGridOntology.ROLE_BATTERY);
        }
        if (config.tickIntervalMs() > 0) {
            chargingTicker = new PriceAwareChargingTicker(this, config.tickIntervalMs() * 2L);
            addBehaviour(chargingTicker);
        }
        auctionResponder = new BatteryAuctionResponder();
        addBehaviour(auctionResponder);

        LOG.fine("Battery " + getLocalName() + " ready (capacity=" + config.capacityKwh() + "kWh, maxPower=" + config.maxPowerKw() + "kW)");
    }

    @Override
    protected void takeDown() {
        recordState();
        if (chargingTicker != null) {
            chargingTicker.stop();
            removeBehaviour(chargingTicker);
        }
        if (auctionResponder != null) {
            removeBehaviour(auctionResponder);
        }
        try {
            SmartGridOntology.deregisterFromDF(this);
            LOG.fine("Battery " + getLocalName() + " stopped. Discharged="
                + String.format("%.4f", totalKwhDischarged) + "kWh, Charged=" + String.format("%.4f", totalKwhCharged)
                + "kWh, SoC=" + String.format("%.1f%%", getSoC() * 100));
        } catch (Throwable ignored) {}
    }

    public double getSoC() {
        if (config == null || config.capacityKwh() <= 0) {
            return 0.70;
        }
        return storedKwh / config.capacityKwh();
    }

    private class PriceAwareChargingTicker extends TickerBehaviour {

        private final long periodMs;

        PriceAwareChargingTicker(Agent a, long period) {
            super(a, period);
            this.periodMs = period;
        }

        @Override
        protected void onTick() {
            if (config.maxRounds() > 0 && stepCount >= config.maxRounds()) {
                stop();
                return;
            }
            stepCount++;
            // Charges when electricity prices are low (<= $0.12/kWh) or negative, and grid frequency is stable
            boolean lowOrNegativePrice = (lastSeenMarketPrice <= 0.12);
            boolean frequencyHealthy = (lastSeenGridFrequency >= 49.85);

            if (lowOrNegativePrice && frequencyHealthy && getSoC() < 0.95) {
                // Maximum charge rate if price is negative or very low (super off-peak absorption)
                double chargeMultiplier = (lastSeenMarketPrice <= 0.05) ? 1.5 : 1.0;
                double chargeKw = Math.min(config.maxPowerKw() * 0.5, config.maxPowerKw() * chargeMultiplier);
                double chargedKwh = SmartGridOntology.kwToKwh(chargeKw, periodMs);
                storedKwh = Math.min(config.capacityKwh() * 0.95, storedKwh + chargedKwh);
                totalKwhCharged += chargedKwh;
                recordState();
            }
        }
    }

    private class BatteryAuctionResponder extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchProtocol(SmartGridOntology.PROTOCOL_CONTRACT_NET),
                MessageTemplate.MatchOntology(SmartGridOntology.ONTOLOGY_ENERGY_RESERVE)
            );

            ACLMessage msg = myAgent.receive(mt);
            while (msg != null) {
                int perf = msg.getPerformative();
                if (perf == ACLMessage.CFP) {
                    handleCfp(msg);
                } else if (perf == ACLMessage.ACCEPT_PROPOSAL) {
                    handleAccept(msg);
                } else if (perf == ACLMessage.REJECT_PROPOSAL) {
                    LOG.log(Level.FINEST, "Battery proposal rejected for " + getLocalName());
                }
                msg = myAgent.receive(mt);
            }
            block();
        }

        private void handleCfp(ACLMessage cfp) {
            ACLMessage reply = cfp.createReply();
            try {
                SmartGridOntology.CfpPayload cfpData = SmartGridOntology.CfpPayload.fromJson(cfp.getContent());
                lastSeenGridFrequency = cfpData.gridFrequency;
                lastSeenMarketPrice = cfpData.marketPricePerKwh;

                // Handle surplus feed-in absorption: charge battery from surplus generation
                if (SmartGridOntology.ACTION_FEED_IN.equals(cfpData.action)) {
                    double soc = getSoC();
                    if (soc < 0.95) { // Headroom available for charging
                        double availableHeadroomKwh = (0.95 * config.capacityKwh()) - storedKwh;
                        double maxChargeKw = availableHeadroomKwh / (config.tickIntervalMs() / 3600000.0);
                        double offeredKw = Math.min(config.maxPowerKw(), maxChargeKw);

                        if (offeredKw >= 0.5) {
                            reply.setPerformative(ACLMessage.PROPOSE);
                            SmartGridOntology.ProposePayload payload = new SmartGridOntology.ProposePayload(
                                SmartGridOntology.ACTION_FEED_IN,
                                Math.round(offeredKw * 100.0) / 100.0,
                                Math.round(cfpData.marketPricePerKwh * 1000.0) / 1000.0,
                                SmartGridOntology.ROLE_BATTERY,
                                Math.round(soc * 100.0) / 100.0
                            );
                            reply.setContent(payload.toJson());
                        } else {
                            reply.setPerformative(ACLMessage.REFUSE);
                        }
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                    myAgent.send(reply);
                    return;
                }

                // Battery only bids for reserve-power generation, not load shedding
                if (!SmartGridOntology.ACTION_RESERVE_POWER.equals(cfpData.action)) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    myAgent.send(reply);
                    return;
                }

                double soc = getSoC();
                if (soc > 0.15) { // Minimum 15% reserve limit
                    double availableKwh = storedKwh - (0.10 * config.capacityKwh());
                    double maxDischargeKw = availableKwh / (config.tickIntervalMs() / 3600000.0);
                    double offeredKw = Math.min(config.maxPowerKw(), maxDischargeKw);

                    // Dynamic pricing: cheaper if battery is full, more expensive if depleted
                    double price = 0.15 + (0.06 * (1.0 - soc));

                    reply.setPerformative(ACLMessage.PROPOSE);
                    SmartGridOntology.ProposePayload payload = new SmartGridOntology.ProposePayload(
                        SmartGridOntology.ACTION_RESERVE_POWER,
                        Math.round(offeredKw * 100.0) / 100.0,
                        Math.round(price * 1000.0) / 1000.0,
                        SmartGridOntology.ROLE_BATTERY,
                        Math.round(soc * 100.0) / 100.0
                    );
                    reply.setContent(payload.toJson());
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                }
            } catch (Exception e) {
                reply.setPerformative(ACLMessage.REFUSE);
            }
            myAgent.send(reply);
        }

        private void handleAccept(ACLMessage accept) {
            try {
                SmartGridOntology.AcceptPayload p = SmartGridOntology.AcceptPayload.fromJson(accept.getContent());
                if (SmartGridOntology.ACTION_FEED_IN.equals(p.action)) {
                    double chargedKwh = SmartGridOntology.kwToKwh(p.acceptedKw, config.tickIntervalMs());
                    storedKwh = Math.min(config.capacityKwh() * 0.95, storedKwh + chargedKwh);
                    totalKwhCharged += chargedKwh;
                    recordState();

                    SmartGridOntology.sendContractNetInform(myAgent, accept, "charged", p.acceptedKw);
                    LOG.info(getLocalName() + " [CHARGE-AUCTION] Absorbed " + String.format("%.1f", p.acceptedKw)
                        + "kW surplus | SoC=" + String.format("%.1f%%", getSoC() * 100)
                        + " | Cumul Charged=" + String.format("%.4f", totalKwhCharged) + "kWh");
                    return;
                }

                double dischargedKwh = SmartGridOntology.kwToKwh(p.acceptedKw, config.tickIntervalMs());
                storedKwh = Math.max(0.0, storedKwh - dischargedKwh);
                totalKwhDischarged += dischargedKwh;
                recordState();

                SmartGridOntology.sendContractNetInform(myAgent, accept, "delivered", p.acceptedKw);
                LOG.info(getLocalName() + " [DISCHARGE] Dispatched " + String.format("%.1f", p.acceptedKw)
                    + "kW to grid | SoC=" + String.format("%.1f%%", getSoC() * 100)
                    + " | Cumul Discharged=" + String.format("%.4f", totalKwhDischarged) + "kWh");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error processing accept proposal in battery " + getLocalName(), e);
            }
        }
    }

    public double getStoredKwh() {
        return storedKwh;
    }

    public double getTotalKwhDischarged() {
        return totalKwhDischarged;
    }

    public double getTotalKwhCharged() {
        return totalKwhCharged;
    }

    public void setMarketPriceForTest(double price) {
        this.lastSeenMarketPrice = price;
    }
}
