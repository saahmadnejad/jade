package io.donbee.jade.examples.smartgrid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.core.behaviours.Behaviour;
import io.donbee.jade.core.behaviours.CyclicBehaviour;
import io.donbee.jade.core.behaviours.TickerBehaviour;
import io.donbee.jade.lang.acl.ACLMessage;
import io.donbee.jade.lang.acl.MessageTemplate;

/**
 * Grid operator and auctioneer for the microgrid.
 * Monitors grid frequency, detects generation deficits, initiates FIPA Contract-Net
 * burst auctions to purchase reserve energy from prosumers and battery storage systems,
 * and executes FIPA Contract-Net load shedding auctions with flexible consumers.
 */
public class SubstationAgent extends Agent {

    private static final Logger LOG = io.donbee.jade.util.Logger.getJADELogger(SubstationAgent.class.getName());

    public static class Metrics {
        private int auctionsHeld;
        private int proposalsReceived;
        private int acceptedBids;
        private double totalKwhTraded;
        private int demandResponseEvents;
        private double lastFrequency = 50.0;
        private double clearingPrice = 0.15;
        private int completedRounds = 0;

        public synchronized void recordRound() {
            this.completedRounds++;
        }

        public synchronized int getCompletedRounds() {
            return this.completedRounds;
        }

        public synchronized void recordAuction() {
            this.auctionsHeld++;
        }

        public synchronized void recordProposals(int count) {
            this.proposalsReceived += count;
        }

        public synchronized void recordAcceptedBid(double kw, double kwh) {
            this.acceptedBids++;
            this.totalKwhTraded += kwh;
        }

        public synchronized void recordDemandResponseEvent() {
            this.demandResponseEvents++;
        }

        public synchronized void updateFrequency(double frequency) {
            this.lastFrequency = frequency;
        }

        public synchronized void updateClearingPrice(double price) {
            this.clearingPrice = price;
        }

        public synchronized int getAuctionsHeld() { return auctionsHeld; }
        public synchronized int getProposalsReceived() { return proposalsReceived; }
        public synchronized int getAcceptedBids() { return acceptedBids; }
        public synchronized double getTotalKwhTraded() { return totalKwhTraded; }
        public synchronized int getDemandResponseEvents() { return demandResponseEvents; }
        public synchronized double getLastFrequency() { return lastFrequency; }
        public synchronized double getClearingPrice() { return clearingPrice; }
    }

    private static final Map<String, Metrics> METRICS_REGISTRY = new ConcurrentHashMap<>();

    public static Metrics getMetrics(String gridId) {
        return METRICS_REGISTRY.get(gridId);
    }

    public static void clearMetrics() {
        METRICS_REGISTRY.clear();
    }

    private SmartGridOntology.SubstationConfig config;
    private int currentRound = 0;
    private double gridFrequency = 50.0; // Nominal 50.0 Hz
    private double currentMarketPrice = 0.15;
    private boolean demandResponseActive = false;
    private Metrics metrics;

    private final Map<String, Double> telemetryGeneration = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Double> telemetryConsumption = new java.util.concurrent.ConcurrentHashMap<>();

    private final List<AID> cachedProviders = new ArrayList<>();
    private final List<AID> cachedConsumers = new ArrayList<>();
    private long lastProviderSearchMs = 0;
    private long lastConsumerSearchMs = 0;

    private String getAgentNamePrefix() {
        String myName = getLocalName();
        return myName.endsWith("substation") ? myName.substring(0, myName.length() - "substation".length()) : "";
    }

    private List<AID> getSurplusAbsorbers() {
        List<AID> absorbers = new ArrayList<>(getConsumers());
        if (config.batteriesCount() > 0) {
            String prefix = getAgentNamePrefix();
            for (int k = 1; k <= config.batteriesCount(); k++) {
                absorbers.add(new AID(prefix + "battery-" + k, AID.ISLOCALNAME));
            }
        }
        return absorbers;
    }

    private List<AID> getProviders() {
        if (!cachedProviders.isEmpty()) {
            return cachedProviders;
        }
        long now = System.currentTimeMillis();
        if (now - lastProviderSearchMs > 3000) {
            List<AID> found = SmartGridOntology.findAgents(this, SmartGridOntology.DF_SERVICE_PROVIDER, config.gridId());
            if (!found.isEmpty()) {
                cachedProviders.clear();
                cachedProviders.addAll(found);
                lastProviderSearchMs = now;
                return cachedProviders;
            }
        }
        if (config.prosumersCount() > 0 || config.batteriesCount() > 0) {
            String prefix = getAgentNamePrefix();
            for (int i = 1; i <= config.prosumersCount(); i++) {
                cachedProviders.add(new AID(prefix + "prosumer-" + i, AID.ISLOCALNAME));
            }
            for (int k = 1; k <= config.batteriesCount(); k++) {
                cachedProviders.add(new AID(prefix + "battery-" + k, AID.ISLOCALNAME));
            }
        }
        return cachedProviders;
    }

    private List<AID> getConsumers() {
        if (!cachedConsumers.isEmpty()) {
            return cachedConsumers;
        }
        long now = System.currentTimeMillis();
        if (now - lastConsumerSearchMs > 3000) {
            List<AID> found = SmartGridOntology.findAgents(this, SmartGridOntology.DF_SERVICE_CONSUMER, config.gridId());
            if (!found.isEmpty()) {
                cachedConsumers.clear();
                cachedConsumers.addAll(found);
                lastConsumerSearchMs = now;
                return cachedConsumers;
            }
        }
        if (config.consumersCount() > 0) {
            String prefix = getAgentNamePrefix();
            for (int j = 1; j <= config.consumersCount(); j++) {
                cachedConsumers.add(new AID(prefix + "consumer-" + j, AID.ISLOCALNAME));
            }
        }
        return cachedConsumers;
    }

    public static class BidProposal {
        public final AID sender;
        public final double offeredKw;
        public final double pricePerKwh;
        public final String action;
        public final ACLMessage message;

        public BidProposal(AID sender, double offeredKw, double pricePerKwh, String action, ACLMessage message) {
            this.sender = sender;
            this.offeredKw = offeredKw;
            this.pricePerKwh = pricePerKwh;
            this.action = action;
            this.message = message;
        }
    }

    @Override
    protected void setup() {
        config = SmartGridOntology.SubstationConfig.fromArgs(getArguments());
        metrics = new Metrics();
        METRICS_REGISTRY.put(config.gridId(), metrics);

        SmartGridOntology.registerWithDF(this, SmartGridOntology.DF_SERVICE_SUBSTATION, config.gridId(), null);
        addBehaviour(new TelemetryListener());
        if (config.tickIntervalMs() > 0) {
            addBehaviour(new GridMonitoringTicker(this, config.tickIntervalMs()));
        }
        addBehaviour(new DeliveryConfirmationListener());

        LOG.info("Substation " + getLocalName() + " initialized for grid=" + config.gridId()
            + " (interval=" + config.tickIntervalMs() + "ms, stormProb=" + config.stormProbability()
            + ", maxRounds=" + config.maxRounds() + ", baseDemand=" + config.baselineDemandKw() + "kW)");
    }

    @Override
    protected void takeDown() {
        SmartGridOntology.deregisterFromDF(this);
        LOG.info("Substation " + getLocalName() + " terminated. Stats: auctions=" + metrics.getAuctionsHeld()
            + ", proposalsReceived=" + metrics.getProposalsReceived() + ", acceptedBids=" + metrics.getAcceptedBids()
            + ", totalKwhTraded=" + String.format("%.4f", metrics.getTotalKwhTraded())
            + ", drEvents=" + metrics.getDemandResponseEvents());
        METRICS_REGISTRY.remove(config.gridId());
    }

    private class GridMonitoringTicker extends TickerBehaviour {

        GridMonitoringTicker(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (config.maxRounds() > 0 && currentRound >= config.maxRounds()) {
                LOG.info(String.format(
                    "\n==================== SMART GRID SIMULATION REPORT ====================\n"
                    + "Grid: %s | Completed %d/%d Rounds\n"
                    + "Auctions Held: %d | Proposals Evaluated: %d | Accepted Bids: %d\n"
                    + "Total Reserve Energy Traded: %.4f kWh\n"
                    + "Demand-Response Curtailment Events: %d\n"
                    + "Final Grid Frequency: %.2f Hz | Final Clearing Price: $%.2f/kWh\n"
                    + "======================================================================",
                    config.gridId(), currentRound, config.maxRounds(),
                    metrics.getAuctionsHeld(), metrics.getProposalsReceived(), metrics.getAcceptedBids(),
                    metrics.getTotalKwhTraded(), metrics.getDemandResponseEvents(),
                    gridFrequency, currentMarketPrice
                ));
                stop();
                return;
            }
            currentRound++;
            metrics.recordRound();

            int simulatedHour = (int) ((currentRound * 24.0 / Math.max(24, config.maxRounds())) % 24);

            double liveGeneration = telemetryGeneration.values().stream().mapToDouble(Double::doubleValue).sum();
            double liveConsumption = telemetryConsumption.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalDemand = liveConsumption > 0 ? liveConsumption : config.baselineDemandKw();
            double netImbalanceKw = totalDemand - liveGeneration; // >0 is deficit, <0 is surplus

            // Dynamic grid frequency from live generation and consumption telemetry
            double freqDeviation = -(netImbalanceKw / Math.max(100.0, totalDemand * 5.0));
            gridFrequency = Math.round(Math.max(48.8, Math.min(51.2, 50.0 + freqDeviation)) * 100.0) / 100.0;
            currentMarketPrice = Math.round(Math.max(0.02, Math.min(0.60, 0.12 + (netImbalanceKw / Math.max(100.0, totalDemand * 2.0)))) * 1000.0) / 1000.0;
            metrics.updateFrequency(gridFrequency);
            metrics.updateClearingPrice(currentMarketPrice);

            boolean isStorm = (Math.random() < config.stormProbability());
            boolean isEveningPeak = (simulatedHour >= 17 && simulatedHour <= 21);
            boolean isMorningPeak = (simulatedHour >= 8 && simulatedHour <= 11);
            boolean isSolarNoon = (simulatedHour >= 12 && simulatedHour <= 15);

            if (isStorm) {
                // Emergency contingency event (e.g. storm, cloud front, or transmission outage)
                double contingencyDeficit = 60.0 + (Math.random() * 80.0);
                double deficitKw = Math.max(contingencyDeficit, netImbalanceKw + contingencyDeficit);
                gridFrequency = Math.round((49.0 + (Math.random() * 0.3)) * 100.0) / 100.0;
                currentMarketPrice = Math.round((0.45 + (Math.random() * 0.15)) * 1000.0) / 1000.0;
                metrics.updateFrequency(gridFrequency);
                metrics.updateClearingPrice(currentMarketPrice);

                LOG.warning("grid=" + config.gridId() + " round=" + currentRound + " (Hour " + simulatedHour + ":00)"
                    + " [EMERGENCY CONTINGENCY] Deficit=" + String.format("%.1f", deficitKw)
                    + "kW, Frequency=" + String.format("%.2f", gridFrequency)
                    + "Hz, Price=$" + String.format("%.2f", currentMarketPrice) + "/kWh");

                initiateReservePowerAuction(deficitKw);
                if (gridFrequency < 49.5) {
                    initiateLoadShedAuction(deficitKw * 0.45);
                }
            } else if (isEveningPeak) {
                // Metropolitan evening peak: residential AC, appliances, commercial lighting & EV charging surge
                double deficitKw = Math.max(45.0, netImbalanceKw + (Math.random() * 20.0));
                currentMarketPrice = Math.max(currentMarketPrice, 0.34 + (Math.random() * 0.12));
                metrics.updateClearingPrice(currentMarketPrice);

                LOG.info("grid=" + config.gridId() + " round=" + currentRound + " (Hour " + simulatedHour + ":00)"
                    + " [EVENING PEAK SHAVING] Deficit=" + String.format("%.1f", deficitKw)
                    + "kW, Frequency=" + String.format("%.2f", gridFrequency)
                    + "Hz, Price=$" + String.format("%.2f", currentMarketPrice) + "/kWh");

                initiateReservePowerAuction(deficitKw);
                if (gridFrequency < 49.6) {
                    initiateLoadShedAuction(deficitKw * 0.35);
                }
            } else if (isMorningPeak) {
                // Commercial and industrial ramp: morning demand rise
                double deficitKw = Math.max(25.0, netImbalanceKw);
                currentMarketPrice = Math.max(currentMarketPrice, 0.22 + (Math.random() * 0.08));
                metrics.updateClearingPrice(currentMarketPrice);

                LOG.info("grid=" + config.gridId() + " round=" + currentRound + " (Hour " + simulatedHour + ":00)"
                    + " [MORNING RAMP BALANCING] Deficit=" + String.format("%.1f", deficitKw)
                    + "kW, Frequency=" + String.format("%.2f", gridFrequency)
                    + "Hz, Price=$" + String.format("%.2f", currentMarketPrice) + "/kWh");

                initiateReservePowerAuction(deficitKw);
            } else if (isSolarNoon || netImbalanceKw < -15.0) {
                // Solar surplus midday: solar PV generation peaks, driving price low and charging batteries / flexible loads
                double surplusKw = Math.max(15.0, Math.max(-netImbalanceKw, (config.prosumersCount() * 1.5)));
                gridFrequency = Math.max(50.05, gridFrequency);
                currentMarketPrice = Math.min(currentMarketPrice, 0.05 + (Math.random() * 0.03)); // low price incentivizes battery charging
                metrics.updateFrequency(gridFrequency);
                metrics.updateClearingPrice(currentMarketPrice);

                if (demandResponseActive && gridFrequency >= 49.85) {
                    demandResponseActive = false;
                }

                LOG.info("grid=" + config.gridId() + " round=" + currentRound + " (Hour " + simulatedHour + ":00)"
                    + " [SOLAR SURPLUS & STORAGE] Freq=" + String.format("%.2f", gridFrequency)
                    + "Hz, Price=$" + String.format("%.2f", currentMarketPrice) + "/kWh | Clearing Clean Feed-in");

                // Initiate market auction to trade prosumers' surplus energy and charge batteries
                initiateSurplusAbsorptionAuction(surplusKw);
            } else {
                // Continuous real-time frequency containment reserve (FCR) balancing
                double deficitKw = Math.max(15.0, Math.abs(netImbalanceKw));
                currentMarketPrice = Math.max(0.14, currentMarketPrice);
                metrics.updateClearingPrice(currentMarketPrice);

                initiateReservePowerAuction(deficitKw);
            }
        }

        private void initiateSurplusAbsorptionAuction(double surplusKw) {
            List<AID> absorbers = getSurplusAbsorbers();
            if (absorbers.isEmpty()) {
                return;
            }

            metrics.recordAuction();
            String conversationId = "auction-feedin-r" + currentRound + "-" + System.currentTimeMillis();

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setProtocol(SmartGridOntology.PROTOCOL_CONTRACT_NET);
            cfp.setOntology(SmartGridOntology.ONTOLOGY_ENERGY_RESERVE);
            cfp.setConversationId(conversationId);
            for (AID a : absorbers) {
                cfp.addReceiver(a);
            }

            SmartGridOntology.CfpPayload payload = new SmartGridOntology.CfpPayload(
                SmartGridOntology.ACTION_FEED_IN,
                currentRound,
                surplusKw,
                gridFrequency,
                currentMarketPrice,
                currentMarketPrice
            );
            cfp.setContent(payload.toJson());
            send(cfp);

            long deadline = System.currentTimeMillis() + (long)(config.tickIntervalMs() * 0.8);
            myAgent.addBehaviour(new AuctionResponseCollector(myAgent, conversationId, surplusKw, deadline, absorbers.size(), SmartGridOntology.ACTION_FEED_IN));
        }

        private void initiateReservePowerAuction(double deficitKw) {
            List<AID> providers = getProviders();
            if (providers.isEmpty()) {
                return;
            }

            metrics.recordAuction();
            String conversationId = "auction-reserve-r" + currentRound + "-" + System.currentTimeMillis();

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setProtocol(SmartGridOntology.PROTOCOL_CONTRACT_NET);
            cfp.setOntology(SmartGridOntology.ONTOLOGY_ENERGY_RESERVE);
            cfp.setConversationId(conversationId);
            for (AID p : providers) {
                cfp.addReceiver(p);
            }

            SmartGridOntology.CfpPayload payload = new SmartGridOntology.CfpPayload(
                SmartGridOntology.ACTION_RESERVE_POWER,
                currentRound,
                deficitKw,
                gridFrequency,
                0.50,
                currentMarketPrice
            );
            cfp.setContent(payload.toJson());
            send(cfp);

            long deadline = System.currentTimeMillis() + (long)(config.tickIntervalMs() * 0.8);
            myAgent.addBehaviour(new AuctionResponseCollector(myAgent, conversationId, deficitKw, deadline, providers.size(), SmartGridOntology.ACTION_RESERVE_POWER));
        }

        private void initiateLoadShedAuction(double neededShedKw) {
            List<AID> consumers = getConsumers();
            if (consumers.isEmpty()) {
                return;
            }

            metrics.recordAuction();
            demandResponseActive = true;
            metrics.recordDemandResponseEvent();
            String conversationId = "auction-shed-r" + currentRound + "-" + System.currentTimeMillis();

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setProtocol(SmartGridOntology.PROTOCOL_CONTRACT_NET);
            cfp.setOntology(SmartGridOntology.ONTOLOGY_ENERGY_RESERVE);
            cfp.setConversationId(conversationId);
            for (AID c : consumers) {
                cfp.addReceiver(c);
            }

            SmartGridOntology.CfpPayload payload = new SmartGridOntology.CfpPayload(
                SmartGridOntology.ACTION_SHED_LOAD,
                currentRound,
                neededShedKw,
                gridFrequency,
                0.25, // max curtailment compensation
                currentMarketPrice
            );
            cfp.setContent(payload.toJson());
            send(cfp);

            long deadline = System.currentTimeMillis() + (long)(config.tickIntervalMs() * 0.8);
            myAgent.addBehaviour(new AuctionResponseCollector(myAgent, conversationId, neededShedKw, deadline, consumers.size(), SmartGridOntology.ACTION_SHED_LOAD));
        }
    }

    private class AuctionResponseCollector extends Behaviour {

        private final String conversationId;
        private final double neededKw;
        private final long deadlineMs;
        private final int expectedRespondents;
        private final String auctionAction;
        private final boolean isLoadShed;
        private final List<BidProposal> proposals = new ArrayList<>();
        private int responsesCount = 0;
        private boolean evaluated = false;

        AuctionResponseCollector(Agent a, String conversationId, double neededKw, long deadlineMs,
                                 int expectedRespondents, String auctionAction) {
            super(a);
            this.conversationId = conversationId;
            this.neededKw = neededKw;
            this.deadlineMs = deadlineMs;
            this.expectedRespondents = expectedRespondents;
            this.auctionAction = auctionAction;
            this.isLoadShed = SmartGridOntology.ACTION_SHED_LOAD.equals(auctionAction);
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchConversationId(conversationId),
                MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                )
            );

            ACLMessage msg = myAgent.receive(mt);
            while (msg != null) {
                responsesCount++;
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    try {
                        SmartGridOntology.ProposePayload p = SmartGridOntology.ProposePayload.fromJson(msg.getContent());
                        if (p.offeredKw > 0) {
                            proposals.add(new BidProposal(msg.getSender(), p.offeredKw, p.pricePerKwh, p.action, msg));
                        }
                    } catch (Exception e) {
                        LOG.log(Level.FINE, "Failed parsing proposal from " + msg.getSender().getLocalName(), e);
                    }
                }
                msg = myAgent.receive(mt);
            }

            if (!evaluated && (responsesCount >= expectedRespondents || System.currentTimeMillis() >= deadlineMs)) {
                evaluateAndAward();
                evaluated = true;
            } else if (!evaluated) {
                block(15);
            }
        }

        private void evaluateAndAward() {
            metrics.recordProposals(proposals.size());
            // Award lowest-cost proposals first
            proposals.sort(Comparator.comparingDouble(p -> p.pricePerKwh));

            double allocatedKw = 0.0;
            double hours = config.tickIntervalMs() / 3600000.0;
            int acceptedBidders = 0;

            for (BidProposal proposal : proposals) {
                if (allocatedKw < neededKw) {
                    double acceptedKw = Math.min(proposal.offeredKw, neededKw - allocatedKw);
                    allocatedKw += acceptedKw;
                    acceptedBidders++;
                    metrics.recordAcceptedBid(acceptedKw, acceptedKw * hours);

                    ACLMessage accept = proposal.message.createReply();
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    accept.setProtocol(SmartGridOntology.PROTOCOL_CONTRACT_NET);
                    accept.setOntology(SmartGridOntology.ONTOLOGY_ENERGY_RESERVE);
                    accept.setContent(new SmartGridOntology.AcceptPayload(
                        proposal.action,
                        acceptedKw,
                        proposal.pricePerKwh
                    ).toJson());
                    myAgent.send(accept);
                } else {
                    ACLMessage reject = proposal.message.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    reject.setProtocol(SmartGridOntology.PROTOCOL_CONTRACT_NET);
                    reject.setOntology(SmartGridOntology.ONTOLOGY_ENERGY_RESERVE);
                    myAgent.send(reject);
                }
            }

            double satisfactionRatio = neededKw > 0 ? (allocatedKw / neededKw) : 1.0;
            if (SmartGridOntology.ACTION_FEED_IN.equals(auctionAction)) {
                // Absorbing generation surplus decreases elevated frequency back down toward 50.0 Hz
                gridFrequency = Math.max(50.0, gridFrequency - (0.5 * satisfactionRatio));
            } else {
                // Adding reserve power or shedding load increases depressed frequency back up toward 50.0 Hz
                gridFrequency = Math.min(50.0, gridFrequency + (0.5 * satisfactionRatio));
            }
            gridFrequency = Math.round(gridFrequency * 100.0) / 100.0;
            metrics.updateFrequency(gridFrequency);

            String auctionType = isLoadShed ? "LOAD-SHED" : (SmartGridOntology.ACTION_FEED_IN.equals(auctionAction) ? "FEED-IN" : "RESERVE-POWER");
            LOG.info("grid=" + config.gridId() + " [" + auctionType + " AUCTION CLOSED] Needed="
                + String.format("%.1f", neededKw) + "kW | Bids Received=" + proposals.size()
                + " | Awarded=" + String.format("%.1f", allocatedKw) + "kW across " + acceptedBidders + " bidders"
                + " | Restored Freq=" + String.format("%.2f", gridFrequency) + "Hz"
                + " | Cumulative Traded=" + String.format("%.4f", metrics.getTotalKwhTraded()) + "kWh");
        }

        @Override
        public boolean done() {
            return evaluated;
        }
    }

    private class DeliveryConfirmationListener extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchOntology(SmartGridOntology.ONTOLOGY_ENERGY_RESERVE)
            );
            ACLMessage inform = myAgent.receive(mt);
            while (inform != null) {
                LOG.log(Level.FINE, "Received delivery confirmation from " + inform.getSender().getLocalName());
                inform = myAgent.receive(mt);
            }
            block();
        }
    }

    private class TelemetryListener extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchOntology(SmartGridOntology.ONTOLOGY_GRID_TELEMETRY)
            );
            ACLMessage msg = myAgent.receive(mt);
            while (msg != null) {
                try {
                    SmartGridOntology.TelemetryPayload t = SmartGridOntology.TelemetryPayload.fromJson(msg.getContent());
                    if (t.generationKw > 0) {
                        telemetryGeneration.put(t.agentName, t.generationKw);
                    }
                    if (t.consumptionKw > 0) {
                        telemetryConsumption.put(t.agentName, t.consumptionKw);
                    }
                } catch (Exception ignored) {}
                msg = myAgent.receive(mt);
            }
            block(20);
        }
    }

    public Metrics getMetrics() {
        return metrics;
    }
}
