package io.donbee.jade.examples.smartgrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.rest.scenario.Scenario;
import io.donbee.jade.rest.scenario.ScenarioParam;

/**
 * Smart Grid Microgrid scenario simulating decentralized energy trading,
 * renewable solar/wind prosumers, flexible consumers, and battery energy storage under FIPA Contract-Net.
 * Designed to stress-test high agent density, idle agent footprint, and burst bidding.
 */
public class SmartGridScenario implements Scenario {

    static final String SUBSTATION_CLASS = SubstationAgent.class.getName();
    static final String PROSUMER_CLASS = ProsumerAgent.class.getName();
    static final String CONSUMER_CLASS = ConsumerAgent.class.getName();
    static final String BATTERY_CLASS = BatteryStorageAgent.class.getName();

    @Override
    public String id() {
        return "smart-grid";
    }

    @Override
    public String title() {
        return "Smart Grid Microgrid & Decentralized Energy Trading";
    }

    @Override
    public String description() {
        return "Decentralized microgrid simulation where solar and wind prosumers, smart consumers, "
            + "and battery storage systems trade reserve power and perform demand-response shedding "
            + "via FIPA Contract-Net auctions to balance local grid frequency and demand.";
    }

    @Override
    public List<ScenarioParam> params() {
        return List.of(
            ScenarioParam.intParam("prosumersCount", 100, 1, 5000, "Number of solar/wind prosumer agents in metropolitan microgrid (default: 100, max: 5000)"),
            ScenarioParam.intParam("consumersCount", 100, 1, 5000, "Number of residential/commercial consumer agents (default: 100, max: 5000)"),
            ScenarioParam.intParam("tickIntervalMs", 500, 50, 60000, "Market clearing and telemetry interval in milliseconds (default: 500ms)"),
            ScenarioParam.stringParam("stormProbability", "0.1", "Probability of sudden generation deficit/contingency event (0.0 to 1.0, default: 0.1)")
        );
    }

    @Override
    public List<AgentSpec> agents(Map<String, Object> config) {
        int prosumersCount = getInt(config, "prosumersCount", 100);
        int consumersCount = getInt(config, "consumersCount", 100);
        double stormProbability = getDouble(config, "stormProbability", 0.1);
        int tickIntervalMs = getInt(config, "tickIntervalMs", 500);
        int batteriesCount = getInt(config, "batteriesCount", Math.max(1, prosumersCount / 20));
        int maxRounds = getInt(config, "maxRounds", 48);

        String gridId = "grid-" + System.currentTimeMillis();
        double baselineDemand = consumersCount * 3.5;
        boolean registerWithDf = getBoolean(config, "registerWithDf", true);

        List<AgentSpec> specs = new ArrayList<>();

        // 1 Substation (Grid Operator & Auctioneer)
        SmartGridOntology.SubstationConfig subCfg = new SmartGridOntology.SubstationConfig(
            gridId, tickIntervalMs, stormProbability, maxRounds, baselineDemand,
            prosumersCount, consumersCount, batteriesCount
        );
        specs.add(new AgentSpec("substation", SUBSTATION_CLASS, new ArrayList<>(subCfg.toArgs())));

        // N Prosumers (Mix of solar PV and wind turbines)
        for (int i = 1; i <= prosumersCount; i++) {
            boolean isWind = (i % 3 == 0);
            double peakCapacity = isWind ? (12.0 + ((i % 4) * 5.0)) : (5.0 + ((i % 5) * 2.5));
            SmartGridOntology.ProsumerConfig prosCfg = new SmartGridOntology.ProsumerConfig(
                gridId, i, peakCapacity, tickIntervalMs, isWind, registerWithDf, maxRounds
            );
            specs.add(new AgentSpec("prosumer-" + i, PROSUMER_CLASS, new ArrayList<>(prosCfg.toArgs())));
        }

        // M Consumers (Participating in demand-response)
        for (int j = 1; j <= consumersCount; j++) {
            double baseDemand = 2.0 + ((j % 4) * 1.5); // 2.0 to 6.5 kW
            SmartGridOntology.ConsumerConfig consCfg = new SmartGridOntology.ConsumerConfig(
                gridId, j, baseDemand, tickIntervalMs, registerWithDf, maxRounds
            );
            specs.add(new AgentSpec("consumer-" + j, CONSUMER_CLASS, new ArrayList<>(consCfg.toArgs())));
        }

        // K Batteries (Dispatchable reserve storage)
        for (int k = 1; k <= batteriesCount; k++) {
            double capacityKwh = 50.0 + ((k % 3) * 25.0); // 50 to 100 kWh
            double maxPowerKw = 20.0 + ((k % 3) * 10.0);  // 20 to 40 kW
            SmartGridOntology.BatteryConfig batCfg = new SmartGridOntology.BatteryConfig(
                gridId, k, capacityKwh, maxPowerKw, tickIntervalMs, registerWithDf, maxRounds
            );
            specs.add(new AgentSpec("battery-" + k, BATTERY_CLASS, new ArrayList<>(batCfg.toArgs())));
        }

        return specs;
    }

    private static <T> T parseConfig(Map<String, Object> config, String key, T defaultValue,
                                     Function<Number, T> fromNumber,
                                     Function<String, T> fromString) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object val = config.get(key);
        if (val instanceof Number) {
            return fromNumber.apply((Number) val);
        }
        try {
            return fromString.apply(String.valueOf(val).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static int getInt(Map<String, Object> config, String key, int defaultValue) {
        return parseConfig(config, key, defaultValue, Number::intValue, Integer::parseInt);
    }

    private static double getDouble(Map<String, Object> config, String key, double defaultValue) {
        return parseConfig(config, key, defaultValue, Number::doubleValue, Double::parseDouble);
    }

    private static boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object val = config.get(key);
        if (val instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(val).trim());
    }
}
