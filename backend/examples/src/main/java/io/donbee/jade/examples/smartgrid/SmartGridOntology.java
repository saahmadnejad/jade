package io.donbee.jade.examples.smartgrid;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.donbee.jade.core.AID;
import io.donbee.jade.core.Agent;
import io.donbee.jade.domain.DFService;
import io.donbee.jade.domain.FIPAException;
import io.donbee.jade.domain.FIPAAgentManagement.DFAgentDescription;
import io.donbee.jade.domain.FIPAAgentManagement.Property;
import io.donbee.jade.domain.FIPAAgentManagement.ServiceDescription;
import io.donbee.jade.domain.FIPANames;
import io.donbee.jade.lang.acl.ACLMessage;
import io.vertx.core.json.JsonObject;

/**
 * Protocol, ontology, typed configs, typed payloads, and Directory Facilitator helpers
 * for the Smart Grid Microgrid scenario.
 */
public final class SmartGridOntology {

    private static final Logger LOG = io.donbee.jade.util.Logger.getJADELogger(SmartGridOntology.class.getName());

    private SmartGridOntology() {}

    public static final String ONTOLOGY_ENERGY_RESERVE = "smartgrid-energy-reserve";
    public static final String ONTOLOGY_GRID_TELEMETRY = "smartgrid-grid-telemetry";
    public static final String PROTOCOL_CONTRACT_NET = FIPANames.InteractionProtocol.FIPA_CONTRACT_NET;

    public static final String DF_SERVICE_PROVIDER = "smartgrid-power-provider";
    public static final String DF_SERVICE_CONSUMER = "smartgrid-power-consumer";
    public static final String DF_SERVICE_SUBSTATION = "smartgrid-substation";

    public static final String PROP_GRID = "grid";
    public static final String PROP_ROLE = "role";

    public static final String ROLE_PROSUMER = "prosumer";
    public static final String ROLE_BATTERY = "battery";
    public static final String ROLE_CONSUMER = "consumer";

    public static final String ACTION_RESERVE_POWER = "reserve-power";
    public static final String ACTION_SHED_LOAD = "shed-load";
    public static final String ACTION_FEED_IN = "feed-in";

    // --- Calculation Helpers ---

    public static double kwToKwh(double kw, long periodMs) {
        return kw * (periodMs / 3600000.0);
    }

    // --- Typed Agent Configurations ---

    public record SubstationConfig(
        String gridId,
        int tickIntervalMs,
        double stormProbability,
        int maxRounds,
        double baselineDemandKw,
        int prosumersCount,
        int consumersCount,
        int batteriesCount
    ) {
        public SubstationConfig(String gridId, int tickIntervalMs, double stormProbability,
                                int maxRounds, double baselineDemandKw) {
            this(gridId, tickIntervalMs, stormProbability, maxRounds, baselineDemandKw, 0, 0, 0);
        }

        public List<String> toArgs() {
            return List.of(
                gridId,
                String.valueOf(tickIntervalMs),
                String.valueOf(stormProbability),
                String.valueOf(maxRounds),
                String.valueOf(baselineDemandKw),
                String.valueOf(prosumersCount),
                String.valueOf(consumersCount),
                String.valueOf(batteriesCount)
            );
        }

        public static SubstationConfig fromArgs(Object[] args) {
            return new SubstationConfig(
                argString(args, 0, "default-grid"),
                argInt(args, 1, 500),
                argDouble(args, 2, 0.1),
                argInt(args, 3, 100),
                argDouble(args, 4, 100.0),
                argInt(args, 5, 0),
                argInt(args, 6, 0),
                argInt(args, 7, 0)
            );
        }
    }

    public record ProsumerConfig(
        String gridId,
        int prosumerIndex,
        double peakCapacityKw,
        int tickIntervalMs,
        boolean isWind,
        boolean registerWithDf,
        int maxRounds
    ) {
        public ProsumerConfig(String gridId, int prosumerIndex, double peakCapacityKw,
                              int tickIntervalMs, boolean isWind, boolean registerWithDf) {
            this(gridId, prosumerIndex, peakCapacityKw, tickIntervalMs, isWind, registerWithDf, 48);
        }

        public ProsumerConfig(String gridId, int prosumerIndex, double peakCapacityKw,
                              int tickIntervalMs, boolean isWind) {
            this(gridId, prosumerIndex, peakCapacityKw, tickIntervalMs, isWind, true, 48);
        }

        public List<String> toArgs() {
            return List.of(
                gridId,
                String.valueOf(prosumerIndex),
                String.valueOf(peakCapacityKw),
                String.valueOf(tickIntervalMs),
                String.valueOf(isWind),
                String.valueOf(registerWithDf),
                String.valueOf(maxRounds)
            );
        }

        public static ProsumerConfig fromArgs(Object[] args) {
            return new ProsumerConfig(
                argString(args, 0, "default-grid"),
                argInt(args, 1, 1),
                argDouble(args, 2, 10.0),
                argInt(args, 3, 500),
                argBoolean(args, 4, false),
                argBoolean(args, 5, true),
                argInt(args, 6, 48)
            );
        }
    }

    public record ConsumerConfig(
        String gridId,
        int consumerIndex,
        double baseDemandKw,
        int tickIntervalMs,
        boolean registerWithDf,
        int maxRounds
    ) {
        public ConsumerConfig(String gridId, int consumerIndex, double baseDemandKw,
                              int tickIntervalMs, boolean registerWithDf) {
            this(gridId, consumerIndex, baseDemandKw, tickIntervalMs, registerWithDf, 48);
        }

        public ConsumerConfig(String gridId, int consumerIndex, double baseDemandKw,
                              int tickIntervalMs) {
            this(gridId, consumerIndex, baseDemandKw, tickIntervalMs, true, 48);
        }

        public List<String> toArgs() {
            return List.of(
                gridId,
                String.valueOf(consumerIndex),
                String.valueOf(baseDemandKw),
                String.valueOf(tickIntervalMs),
                String.valueOf(registerWithDf),
                String.valueOf(maxRounds)
            );
        }

        public static ConsumerConfig fromArgs(Object[] args) {
            return new ConsumerConfig(
                argString(args, 0, "default-grid"),
                argInt(args, 1, 1),
                argDouble(args, 2, 3.5),
                argInt(args, 3, 500),
                argBoolean(args, 4, true),
                argInt(args, 5, 48)
            );
        }
    }

    public record BatteryConfig(
        String gridId,
        int batteryIndex,
        double capacityKwh,
        double maxPowerKw,
        int tickIntervalMs,
        boolean registerWithDf,
        int maxRounds
    ) {
        public BatteryConfig(String gridId, int batteryIndex, double capacityKwh,
                             double maxPowerKw, int tickIntervalMs, boolean registerWithDf) {
            this(gridId, batteryIndex, capacityKwh, maxPowerKw, tickIntervalMs, registerWithDf, 48);
        }

        public BatteryConfig(String gridId, int batteryIndex, double capacityKwh,
                             double maxPowerKw, int tickIntervalMs) {
            this(gridId, batteryIndex, capacityKwh, maxPowerKw, tickIntervalMs, true, 48);
        }

        public List<String> toArgs() {
            return List.of(
                gridId,
                String.valueOf(batteryIndex),
                String.valueOf(capacityKwh),
                String.valueOf(maxPowerKw),
                String.valueOf(tickIntervalMs),
                String.valueOf(registerWithDf),
                String.valueOf(maxRounds)
            );
        }

        public static BatteryConfig fromArgs(Object[] args) {
            return new BatteryConfig(
                argString(args, 0, "default-grid"),
                argInt(args, 1, 1),
                argDouble(args, 2, 50.0),
                argDouble(args, 3, 25.0),
                argInt(args, 4, 500),
                argBoolean(args, 5, true),
                argInt(args, 6, 48)
            );
        }
    }

    // --- Argument Parsing Helpers ---

    public static String argString(Object[] args, int index, String defaultVal) {
        if (args != null && args.length > index && args[index] != null) {
            return String.valueOf(args[index]);
        }
        return defaultVal;
    }

    public static int argInt(Object[] args, int index, int defaultVal) {
        if (args != null && args.length > index && args[index] != null) {
            try {
                return Integer.parseInt(String.valueOf(args[index]).trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    public static double argDouble(Object[] args, int index, double defaultVal) {
        if (args != null && args.length > index && args[index] != null) {
            try {
                return Double.parseDouble(String.valueOf(args[index]).trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    public static boolean argBoolean(Object[] args, int index, boolean defaultVal) {
        if (args != null && args.length > index && args[index] != null) {
            return Boolean.parseBoolean(String.valueOf(args[index]).trim());
        }
        return defaultVal;
    }

    // --- Directory Facilitator Helpers ---

    public static void registerWithDF(Agent agent, String serviceType, String gridId, String role) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(agent.getLocalName());
        sd.addProperties(new Property(PROP_GRID, gridId));
        if (role != null) {
            sd.addProperties(new Property(PROP_ROLE, role));
        }
        dfd.addServices(sd);
        try {
            ACLMessage request = DFService.createRequestMessage(agent, agent.getDefaultDF(),
                io.donbee.jade.domain.FIPAAgentManagement.FIPAManagementVocabulary.REGISTER, dfd, null);
            io.donbee.jade.domain.FIPAService.doFipaRequestClient(agent, request, 3000);
        } catch (Throwable e) {
            LOG.log(Level.FINEST, "DF registration for " + agent.getLocalName() + ": " + e.getMessage());
        }
    }

    public static void deregisterFromDF(Agent agent) {
        // Only Substation needs explicit DF deregistration; individual microgrid prosumers/consumers
        // are cleaned up by JADE when the scenario container terminates.
        // Skipping synchronous DF calls for hundreds of agents prevents stalling sequential teardown.
        if (agent instanceof SubstationAgent) {
            try {
                DFService.deregister(agent);
            } catch (Throwable ignored) {}
        }
    }

    public static List<AID> findAgents(Agent agent, String serviceType, String gridId) {
        List<AID> matches = new ArrayList<>();
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(serviceType);
            sd.addProperties(new Property(PROP_GRID, gridId));
            template.addServices(sd);
            DFAgentDescription[] results = DFService.search(agent, null, template, null);
            for (DFAgentDescription d : results) {
                matches.add(d.getName());
            }
        } catch (FIPAException e) {
            LOG.log(Level.WARNING, "Failed searching DF for " + serviceType, e);
        }
        return matches;
    }

    // --- Shared Messaging Helpers ---

    public static void sendContractNetInform(Agent agent, ACLMessage acceptMsg, String status, double deliveredKw) {
        ACLMessage inform = acceptMsg.createReply();
        inform.setPerformative(ACLMessage.INFORM);
        inform.setProtocol(PROTOCOL_CONTRACT_NET);
        inform.setOntology(acceptMsg.getOntology());
        inform.setContent(new InformPayload(status, deliveredKw).toJson());
        agent.send(inform);
    }

    // --- Typed Message Payloads ---

    public static class CfpPayload {
        public final String action;
        public final int round;
        public final double deficitKw;
        public final double gridFrequency;
        public final double maxPricePerKwh;
        public final double marketPricePerKwh;

        public CfpPayload(String action, int round, double deficitKw, double gridFrequency,
                          double maxPricePerKwh, double marketPricePerKwh) {
            this.action = action;
            this.round = round;
            this.deficitKw = deficitKw;
            this.gridFrequency = gridFrequency;
            this.maxPricePerKwh = maxPricePerKwh;
            this.marketPricePerKwh = marketPricePerKwh;
        }

        public CfpPayload(int round, double deficitKw, double gridFrequency, double maxPricePerKwh) {
            this(ACTION_RESERVE_POWER, round, deficitKw, gridFrequency, maxPricePerKwh, 0.15);
        }

        public String toJson() {
            return new JsonObject()
                .put("action", action)
                .put("round", round)
                .put("deficitKw", deficitKw)
                .put("gridFrequency", gridFrequency)
                .put("maxPricePerKwh", maxPricePerKwh)
                .put("marketPricePerKwh", marketPricePerKwh)
                .encode();
        }

        public static CfpPayload fromJson(String json) {
            if (json == null || json.trim().isEmpty()) {
                return new CfpPayload(ACTION_RESERVE_POWER, 0, 0.0, 50.0, 0.50, 0.15);
            }
            try {
                JsonObject o = new JsonObject(json);
                return new CfpPayload(
                    o.getString("action", ACTION_RESERVE_POWER),
                    o.getInteger("round", 0),
                    o.getDouble("deficitKw", 0.0),
                    o.getDouble("gridFrequency", 50.0),
                    o.getDouble("maxPricePerKwh", 0.50),
                    o.getDouble("marketPricePerKwh", 0.15)
                );
            } catch (Throwable t) {
                return new CfpPayload(ACTION_RESERVE_POWER, 0, 0.0, 50.0, 0.50, 0.15);
            }
        }
    }

    public static class ProposePayload {
        public final String action;
        public final double offeredKw;
        public final double pricePerKwh;
        public final String sellerRole;
        public final double soc;

        public ProposePayload(String action, double offeredKw, double pricePerKwh, String sellerRole, double soc) {
            this.action = action;
            this.offeredKw = offeredKw;
            this.pricePerKwh = pricePerKwh;
            this.sellerRole = sellerRole;
            this.soc = soc;
        }

        public ProposePayload(double offeredKw, double pricePerKwh, String sellerRole, double soc) {
            this(ACTION_RESERVE_POWER, offeredKw, pricePerKwh, sellerRole, soc);
        }

        public String toJson() {
            return new JsonObject()
                .put("action", action)
                .put("offeredKw", offeredKw)
                .put("pricePerKwh", pricePerKwh)
                .put("sellerRole", sellerRole)
                .put("soc", soc)
                .encode();
        }

        public static ProposePayload fromJson(String json) {
            if (json == null || json.trim().isEmpty()) {
                return new ProposePayload(ACTION_RESERVE_POWER, 0.0, 0.20, ROLE_PROSUMER, 1.0);
            }
            try {
                JsonObject o = new JsonObject(json);
                return new ProposePayload(
                    o.getString("action", ACTION_RESERVE_POWER),
                    o.getDouble("offeredKw", 0.0),
                    o.getDouble("pricePerKwh", 0.20),
                    o.getString("sellerRole", ROLE_PROSUMER),
                    o.getDouble("soc", 1.0)
                );
            } catch (Throwable t) {
                return new ProposePayload(ACTION_RESERVE_POWER, 0.0, 0.20, ROLE_PROSUMER, 1.0);
            }
        }
    }

    public static class AcceptPayload {
        public final String action;
        public final double acceptedKw;
        public final double pricePerKwh;

        public AcceptPayload(String action, double acceptedKw, double pricePerKwh) {
            this.action = action;
            this.acceptedKw = acceptedKw;
            this.pricePerKwh = pricePerKwh;
        }

        public AcceptPayload(double acceptedKw, double pricePerKwh) {
            this(ACTION_RESERVE_POWER, acceptedKw, pricePerKwh);
        }

        public String toJson() {
            return new JsonObject()
                .put("action", action)
                .put("acceptedKw", acceptedKw)
                .put("pricePerKwh", pricePerKwh)
                .encode();
        }

        public static AcceptPayload fromJson(String json) {
            if (json == null || json.trim().isEmpty()) {
                return new AcceptPayload(ACTION_RESERVE_POWER, 0.0, 0.20);
            }
            try {
                JsonObject o = new JsonObject(json);
                return new AcceptPayload(
                    o.getString("action", ACTION_RESERVE_POWER),
                    o.getDouble("acceptedKw", 0.0),
                    o.getDouble("pricePerKwh", 0.20)
                );
            } catch (Throwable t) {
                return new AcceptPayload(ACTION_RESERVE_POWER, 0.0, 0.20);
            }
        }
    }

    public static class InformPayload {
        public final String status;
        public final double deliveredKw;

        public InformPayload(String status, double deliveredKw) {
            this.status = status;
            this.deliveredKw = deliveredKw;
        }

        public String toJson() {
            return new JsonObject()
                .put("status", status)
                .put("deliveredKw", deliveredKw)
                .encode();
        }

        public static InformPayload fromJson(String json) {
            if (json == null || json.trim().isEmpty()) {
                return new InformPayload("delivered", 0.0);
            }
            try {
                JsonObject o = new JsonObject(json);
                return new InformPayload(
                    o.getString("status", "delivered"),
                    o.getDouble("deliveredKw", 0.0)
                );
            } catch (Throwable t) {
                return new InformPayload("delivered", 0.0);
            }
        }
    }

    public static class DemandResponsePayload {
        public final boolean shed;
        public final double gridFrequency;
        public final double marketPricePerKwh;

        public DemandResponsePayload(boolean shed, double gridFrequency, double marketPricePerKwh) {
            this.shed = shed;
            this.gridFrequency = gridFrequency;
            this.marketPricePerKwh = marketPricePerKwh;
        }

        public DemandResponsePayload(boolean shed, double gridFrequency) {
            this(shed, gridFrequency, 0.15);
        }

        public String toJson() {
            return new JsonObject()
                .put("shed", shed)
                .put("gridFrequency", gridFrequency)
                .put("marketPricePerKwh", marketPricePerKwh)
                .encode();
        }

        public static DemandResponsePayload fromJson(String json) {
            if (json == null || json.trim().isEmpty()) {
                return new DemandResponsePayload(false, 50.0, 0.15);
            }
            try {
                JsonObject o = new JsonObject(json);
                return new DemandResponsePayload(
                    o.getBoolean("shed", false),
                    o.getDouble("gridFrequency", 50.0),
                    o.getDouble("marketPricePerKwh", 0.15)
                );
            } catch (Throwable t) {
                return new DemandResponsePayload(false, 50.0, 0.15);
            }
        }
    }

    public static class TelemetryPayload {
        public final String agentName;
        public final String role;
        public final double generationKw;
        public final double consumptionKw;
        public final double netKw;

        public TelemetryPayload(String agentName, String role, double generationKw, double consumptionKw, double netKw) {
            this.agentName = agentName;
            this.role = role;
            this.generationKw = generationKw;
            this.consumptionKw = consumptionKw;
            this.netKw = netKw;
        }

        public String toJson() {
            return new JsonObject()
                .put("agentName", agentName)
                .put("role", role)
                .put("generationKw", generationKw)
                .put("consumptionKw", consumptionKw)
                .put("netKw", netKw)
                .encode();
        }

        public static TelemetryPayload fromJson(String json) {
            if (json == null || json.trim().isEmpty()) {
                return new TelemetryPayload("unknown", ROLE_PROSUMER, 0.0, 0.0, 0.0);
            }
            try {
                JsonObject o = new JsonObject(json);
                return new TelemetryPayload(
                    o.getString("agentName", "unknown"),
                    o.getString("role", ROLE_PROSUMER),
                    o.getDouble("generationKw", 0.0),
                    o.getDouble("consumptionKw", 0.0),
                    o.getDouble("netKw", 0.0)
                );
            } catch (Throwable t) {
                return new TelemetryPayload("unknown", ROLE_PROSUMER, 0.0, 0.0, 0.0);
            }
        }
    }
}
