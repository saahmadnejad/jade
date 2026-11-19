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
 * Prosumer agent modeling residential/commercial renewable generators (solar PV and wind turbines).
 * Simulates real-time generation curves, satisfies local consumption, and sells surplus energy
 * via FIPA Contract-Net auctions.
 */
public class ProsumerAgent extends Agent {

    private static final Logger LOG = io.donbee.jade.util.Logger.getJADELogger(ProsumerAgent.class.getName());

    private SmartGridOntology.ProsumerConfig config;
    private double currentGenerationKw = 0.0;
    private double currentConsumptionKw = 2.0;
    private double surplusKw = 0.0;
    private double totalKwhSold = 0.0;
    private int stepCount = 0;

    private AID substationAID;

    private AID getSubstationAID() {
        if (substationAID != null) {
            return substationAID;
        }
        String myName = getLocalName();
        String prefix = myName.contains("prosumer-") ? myName.substring(0, myName.indexOf("prosumer-")) : "";
        substationAID = new AID(prefix + "substation", AID.ISLOCALNAME);
        return substationAID;
    }

    private RenewableGenerationTicker generationTicker;
    private AuctionResponderBehaviour auctionResponder;

    @Override
    protected void setup() {
        config = SmartGridOntology.ProsumerConfig.fromArgs(getArguments());

        if (config.registerWithDf()) {
            SmartGridOntology.registerWithDF(this, SmartGridOntology.DF_SERVICE_PROVIDER, config.gridId(), SmartGridOntology.ROLE_PROSUMER);
        }
        if (config.tickIntervalMs() > 0) {
            generationTicker = new RenewableGenerationTicker(this, config.tickIntervalMs());
            addBehaviour(generationTicker);
        }
        auctionResponder = new AuctionResponderBehaviour();
        addBehaviour(auctionResponder);

        LOG.fine("Prosumer " + getLocalName() + " ready (" + (config.isWind() ? "wind" : "solar")
            + ", capacity=" + config.peakCapacityKw() + "kW)");
    }

    @Override
    protected void takeDown() {
        if (generationTicker != null) {
            generationTicker.stop();
            removeBehaviour(generationTicker);
        }
        if (auctionResponder != null) {
            removeBehaviour(auctionResponder);
        }
        try {
            SmartGridOntology.deregisterFromDF(this);
            LOG.fine("Prosumer " + getLocalName() + " stopped. Total sold=" + String.format("%.4f", totalKwhSold) + "kWh");
        } catch (Throwable ignored) {}
    }

    private class RenewableGenerationTicker extends TickerBehaviour {

        RenewableGenerationTicker(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (config.maxRounds() > 0 && stepCount >= config.maxRounds()) {
                stop();
                return;
            }
            stepCount++;
            if (config.isWind()) {
                // Wind generation: continuous day/night with Weibull-like gust fluctuations
                double windFactor = 0.3 + (0.5 * Math.sin(stepCount * 0.3) * Math.sin(stepCount * 0.3)) + (Math.random() * 0.35);
                currentGenerationKw = config.peakCapacityKw() * Math.min(1.0, Math.max(0.05, windFactor));
            } else {
                // Solar generation: diurnal sinusoidal curve
                double sunFactor = Math.max(0.0, Math.sin((stepCount % 24) * Math.PI / 12.0));
                double weatherVariation = 0.80 + (Math.random() * 0.35);
                currentGenerationKw = config.peakCapacityKw() * sunFactor * weatherVariation;
            }

            // Household/commercial load
            currentConsumptionKw = 1.0 + (Math.random() * 2.5);
            surplusKw = Math.max(0.0, currentGenerationKw - currentConsumptionKw);

            // Send grid load telemetry to Substation
            ACLMessage telemetry = new ACLMessage(ACLMessage.INFORM);
            telemetry.addReceiver(getSubstationAID());
            telemetry.setOntology(SmartGridOntology.ONTOLOGY_GRID_TELEMETRY);
            telemetry.setContent(new SmartGridOntology.TelemetryPayload(
                getLocalName(),
                SmartGridOntology.ROLE_PROSUMER,
                Math.round(currentGenerationKw * 100.0) / 100.0,
                Math.round(currentConsumptionKw * 100.0) / 100.0,
                Math.round((currentGenerationKw - currentConsumptionKw) * 100.0) / 100.0
            ).toJson());
            send(telemetry);
        }
    }

    private class AuctionResponderBehaviour extends CyclicBehaviour {

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
                // Prosumers sell reserve power or feed in surplus solar/wind energy
                boolean isEligibleAction = SmartGridOntology.ACTION_RESERVE_POWER.equals(payload.action)
                    || SmartGridOntology.ACTION_FEED_IN.equals(payload.action);
                if (!isEligibleAction) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    myAgent.send(reply);
                    return;
                }

                if (surplusKw >= 0.5) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    double basePrice = config.isWind() ? 0.09 : 0.12; // Wind marginal cost slightly cheaper
                    double price = basePrice + ((config.prosumerIndex() % 10) * 0.01) + (Math.random() * 0.02);
                    SmartGridOntology.ProposePayload proposePayload = new SmartGridOntology.ProposePayload(
                        payload.action,
                        Math.round(surplusKw * 100.0) / 100.0,
                        Math.round(price * 1000.0) / 1000.0,
                        SmartGridOntology.ROLE_PROSUMER,
                        1.0
                    );
                    reply.setContent(proposePayload.toJson());
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
                double soldKwh = SmartGridOntology.kwToKwh(p.acceptedKw, config.tickIntervalMs());
                totalKwhSold += soldKwh;
                surplusKw = Math.max(0.0, surplusKw - p.acceptedKw);

                SmartGridOntology.sendContractNetInform(myAgent, accept, "delivered", p.acceptedKw);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error processing accept proposal in " + getLocalName(), e);
            }
        }
    }

    public double getSurplusKw() {
        return surplusKw;
    }

    public double getTotalKwhSold() {
        return totalKwhSold;
    }

    public boolean isWind() {
        return config.isWind();
    }
}
