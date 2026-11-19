package io.donbee.jade.examples.smartgrid;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.core.behaviours.TickerBehaviour;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;

/**
 * Consumer agent modeling residential or commercial flexible electrical loads.
 * Simulates real-time demand, adjusts consumption dynamically based on market price signals,
 * and participates in FIPA Contract-Net auctions to bid load curtailment blocks.
 */
public class ConsumerAgent extends Agent {

    private static final Logger LOG = io.donbee.jade.util.Logger.getJADELogger(ConsumerAgent.class.getName());

    private SmartGridOntology.ConsumerConfig config;
    private double currentDemandKw = 3.5;
    private double totalKwhConsumed = 0.0;
    private double totalKwhCurtailed = 0.0;
    private boolean demandResponseActive = false;
    private double currentCurtailedKw = 0.0;
    private double lastMarketPrice = 0.15;
    private int curtailmentTicksRemaining = 0;
    private AID substationAID;

    private AID getSubstationAID() {
        if (substationAID != null) {
            return substationAID;
        }
        String myName = getLocalName();
        String prefix = myName.contains("consumer-") ? myName.substring(0, myName.indexOf("consumer-")) : "";
        substationAID = new AID(prefix + "substation", AID.ISLOCALNAME);
        return substationAID;
    }

    private DynamicLoadTicker loadTicker;
    private LoadShedContractNetResponder loadShedResponder;
    private int stepCount = 0;

    @Override
    protected void setup() {
        config = SmartGridOntology.ConsumerConfig.fromArgs(getArguments());
        currentDemandKw = config.baseDemandKw();

        if (config.registerWithDf()) {
            SmartGridOntology.registerWithDF(this, SmartGridOntology.DF_SERVICE_CONSUMER, config.gridId(), SmartGridOntology.ROLE_CONSUMER);
        }
        if (config.tickIntervalMs() > 0) {
            loadTicker = new DynamicLoadTicker(this, config.tickIntervalMs());
            addBehaviour(loadTicker);
        }
        loadShedResponder = new LoadShedContractNetResponder();
        addBehaviour(loadShedResponder);

        LOG.fine("Consumer " + getLocalName() + " ready (baseLoad=" + config.baseDemandKw() + "kW)");
    }

    @Override
    protected void takeDown() {
        if (loadTicker != null) {
            loadTicker.stop();
            removeBehaviour(loadTicker);
        }
        if (loadShedResponder != null) {
            removeBehaviour(loadShedResponder);
        }
        try {
            SmartGridOntology.deregisterFromDF(this);
            LOG.fine("Consumer " + getLocalName() + " stopped. Total consumed=" + String.format("%.4f", totalKwhConsumed)
                + "kWh, curtailed=" + String.format("%.4f", totalKwhCurtailed) + "kWh");
        } catch (Throwable ignored) {}
    }

    private class DynamicLoadTicker extends TickerBehaviour {

        private final long periodMs;

        DynamicLoadTicker(Agent a, long period) {
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
            // Stochastic load variation (appliances turning on/off)
            double variation = 0.85 + (Math.random() * 0.30);
            double target = config.baseDemandKw() * variation;

            // Dynamic price-responsiveness: consume more when prices are low/negative, reduce when expensive
            if (lastMarketPrice <= 0.10) {
                target *= 1.15; // Shift flexible loads to cheap periods
            } else if (lastMarketPrice > 0.30) {
                target *= 0.90; // Price-elastic reduction
            }

            // Demand response active curtailment awarded via Contract-Net
            if (demandResponseActive && currentCurtailedKw > 0) {
                target = Math.max(0.5, target - currentCurtailedKw);
                totalKwhCurtailed += SmartGridOntology.kwToKwh(currentCurtailedKw, periodMs);
                curtailmentTicksRemaining--;
                if (curtailmentTicksRemaining <= 0) {
                    demandResponseActive = false;
                    currentCurtailedKw = 0.0;
                }
            }

            currentDemandKw = Math.round(target * 100.0) / 100.0;
            totalKwhConsumed += SmartGridOntology.kwToKwh(currentDemandKw, periodMs);

            // Send grid load telemetry to Substation
            ACLMessage telemetry = new ACLMessage(ACLMessage.INFORM);
            telemetry.addReceiver(getSubstationAID());
            telemetry.setOntology(SmartGridOntology.ONTOLOGY_GRID_TELEMETRY);
            telemetry.setContent(new SmartGridOntology.TelemetryPayload(
                getLocalName(),
                SmartGridOntology.ROLE_CONSUMER,
                0.0,
                currentDemandKw,
                -currentDemandKw
            ).toJson());
            send(telemetry);
        }
    }

    private class LoadShedContractNetResponder extends CyclicBehaviour {

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
                    // Proposal rejected for this auction round; do not arbitrarily cancel ongoing load adjustments
                    LOG.log(Level.FINEST, "Proposal rejected for " + getLocalName());
                }
                msg = myAgent.receive(mt);
            }
            block();
        }

        private void handleCfp(ACLMessage cfp) {
            ACLMessage reply = cfp.createReply();
            try {
                SmartGridOntology.CfpPayload payload = SmartGridOntology.CfpPayload.fromJson(cfp.getContent());
                lastMarketPrice = payload.marketPricePerKwh;

                // Consumer bids curtailment blocks in response to shed-load CFP
                if (SmartGridOntology.ACTION_SHED_LOAD.equals(payload.action)) {
                    double maxCurtailmentKw = currentDemandKw * 0.30; // Up to 30% flexible load
                    if (maxCurtailmentKw >= 0.2) {
                        double incentivePrice = 0.08 + ((config.consumerIndex() % 5) * 0.01);
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(new SmartGridOntology.ProposePayload(
                            SmartGridOntology.ACTION_SHED_LOAD,
                            Math.round(maxCurtailmentKw * 100.0) / 100.0,
                            incentivePrice,
                            SmartGridOntology.ROLE_CONSUMER,
                            1.0
                        ).toJson());
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                } else if (SmartGridOntology.ACTION_FEED_IN.equals(payload.action) && payload.marketPricePerKwh <= 0.08) {
                    // Lowest-cost energy bidding: flexible consumer offers to absorb surplus clean energy
                    double flexibleAddKw = currentDemandKw * 0.25;
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(new SmartGridOntology.ProposePayload(
                        SmartGridOntology.ACTION_FEED_IN,
                        Math.round(flexibleAddKw * 100.0) / 100.0,
                        payload.marketPricePerKwh,
                        SmartGridOntology.ROLE_CONSUMER,
                        1.0
                    ).toJson());
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Failed handling CFP in " + getLocalName(), e);
                reply.setPerformative(ACLMessage.REFUSE);
            }
            myAgent.send(reply);
        }

        private void handleAccept(ACLMessage accept) {
            try {
                SmartGridOntology.AcceptPayload p = SmartGridOntology.AcceptPayload.fromJson(accept.getContent());
                if (SmartGridOntology.ACTION_SHED_LOAD.equals(p.action)) {
                    demandResponseActive = true;
                    currentCurtailedKw = p.acceptedKw;
                    curtailmentTicksRemaining = 3; // Maintain curtailment for 3 ticks
                    SmartGridOntology.sendContractNetInform(myAgent, accept, "curtailed", p.acceptedKw);
                } else if (SmartGridOntology.ACTION_FEED_IN.equals(p.action)) {
                    // Absorbing surplus energy at lowest cost
                    currentDemandKw += p.acceptedKw;
                    SmartGridOntology.sendContractNetInform(myAgent, accept, "absorbed", p.acceptedKw);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error processing accept proposal in " + getLocalName(), e);
            }
        }
    }

    public double getCurrentDemandKw() {
        return currentDemandKw;
    }

    public double getTotalKwhConsumed() {
        return totalKwhConsumed;
    }

    public double getTotalKwhCurtailed() {
        return totalKwhCurtailed;
    }

    public boolean isDemandResponseActive() {
        return demandResponseActive;
    }
}
