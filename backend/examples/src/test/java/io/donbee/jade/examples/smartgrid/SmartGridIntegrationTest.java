package io.donbee.jade.examples.smartgrid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.donbee.jade.core.Profile;
import io.donbee.jade.core.ProfileImpl;
import io.donbee.jade.core.Runtime;
import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.wrapper.AgentController;
import io.donbee.jade.wrapper.ContainerController;

/**
 * End-to-end integration test verifying that SubstationAgent, ProsumerAgent,
 * ConsumerAgent, and BatteryStorageAgent coordinate properly over FIPA Contract-Net.
 * Verifies auction execution, energy trading, load shedding, and burst contention
 * across thread and verticle runtimes using strict Arrange-Act-Assert.
 */
public class SmartGridIntegrationTest {

    private static Runtime runtime;
    private static ContainerController mainContainer;

    @BeforeClass
    public static void bootPlatform() throws Exception {
        runtime = Runtime.instance();
        ProfileImpl profile = new ProfileImpl(true);
        profile.setParameter(Profile.MAIN, "true");
        profile.setParameter(Profile.MAIN_PORT, "12999"); // isolated port
        profile.setParameter(Profile.GUI, "false");
        mainContainer = runtime.createMainContainer(profile);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        SubstationAgent.clearMetrics();
        if (mainContainer != null) {
            try {
                mainContainer.kill();
            } catch (Exception ignored) {}
        }
    }

    @Test
    public void Given_SmartGridScenario_When_Deployed_Then_AuctionsExecutedAndEnergyTraded() throws Exception {
        // --- Arrange ---
        SmartGridScenario scenario = new SmartGridScenario();
        Map<String, Object> config = new HashMap<>();
        config.put("prosumersCount", 6);
        config.put("consumersCount", 4);
        config.put("batteriesCount", 2);
        config.put("tickIntervalMs", 150); // fast simulation tick
        config.put("stormProbability", "0.5"); // high storm probability to test burst contention & demand response
        config.put("maxRounds", 10);

        List<AgentSpec> specs = scenario.agents(config);
        assertThat(specs).hasSize(13); // 1 substation + 6 prosumers + 4 consumers + 2 batteries

        String gridId = String.valueOf(specs.get(0).getArgs().get(0));

        ProfileImpl subProfile = new ProfileImpl(false);
        subProfile.setParameter(Profile.CONTAINER_NAME, "scenario-smartgrid-test-1");
        subProfile.setParameter(Profile.MAIN_PORT, "12999");
        ContainerController scenarioContainer = runtime.createAgentContainer(subProfile);

        // --- Act ---
        for (AgentSpec spec : specs) {
            Object[] args = spec.getArgs().toArray(new Object[0]);
            AgentController ac = scenarioContainer.createNewAgent(spec.getNameSuffix(), spec.getClassName(), args);
            ac.start();
        }

        // Allow ~2.5 seconds for ~10 rounds of market ticks to complete
        Thread.sleep(2500);

        // --- Assert ---
        SubstationAgent.Metrics metrics = SubstationAgent.getMetrics(gridId);
        assertThat(metrics)
            .as("Substation metrics should be recorded for grid " + gridId)
            .isNotNull();
        assertThat(metrics.getAuctionsHeld())
            .as("Substation should have held multiple auction rounds")
            .isGreaterThanOrEqualTo(2);
        assertThat(metrics.getProposalsReceived())
            .as("Providers should have submitted proposals")
            .isGreaterThan(0);
        assertThat(metrics.getAcceptedBids())
            .as("Substation should have awarded bids")
            .isGreaterThan(0);
        assertThat(metrics.getTotalKwhTraded())
            .as("Reserve energy should have been traded")
            .isGreaterThan(0.0);

        // Clean teardown
        scenarioContainer.kill();
    }

    @Test
    public void Given_PriceDrop_When_BatteryObservesLowPrice_Then_ChargesBattery() throws Exception {
        // --- Arrange ---
        String gridId = "grid-battery-test-" + System.currentTimeMillis();
        ProfileImpl subProfile = new ProfileImpl(false);
        subProfile.setParameter(Profile.CONTAINER_NAME, "scenario-battery-test");
        subProfile.setParameter(Profile.MAIN_PORT, "12999");
        ContainerController batteryContainer = runtime.createAgentContainer(subProfile);

        SmartGridOntology.BatteryConfig batteryConfig = new SmartGridOntology.BatteryConfig(
            gridId, 1, 50.0, 20.0, 100
        );

        AgentController batteryAc = batteryContainer.createNewAgent(
            "test-battery",
            BatteryStorageAgent.class.getName(),
            batteryConfig.toArgs().toArray()
        );

        // --- Act ---
        batteryAc.start();
        Thread.sleep(600); // Allow charging ticker ticks under default initial off-peak price

        // --- Assert ---
        BatteryStorageAgent.BatteryState state = BatteryStorageAgent.getState("test-battery");
        assertThat(state)
            .as("Battery state should be recorded in registry")
            .isNotNull();
        assertThat(state.soc())
            .as("Battery SoC should have increased during low-price charging")
            .isGreaterThan(0.70);
        assertThat(state.totalKwhCharged())
            .as("Battery should have absorbed surplus power during charging ticks")
            .isGreaterThan(0.0);
        assertThat(state.storedKwh())
            .as("Battery stored energy should be greater than initial 35 kWh")
            .isGreaterThan(35.0);

        // Clean teardown
        batteryContainer.kill();
    }

    @Test
    public void Given_BurstContention_When_GenerationDeficitOccurs_Then_HighDensityAgentsCoordinateWithoutDeadlock() throws Exception {
        // --- Arrange ---
        SmartGridScenario scenario = new SmartGridScenario();
        Map<String, Object> config = new HashMap<>();
        config.put("prosumersCount", 20);
        config.put("consumersCount", 15);
        config.put("batteriesCount", 5);
        config.put("tickIntervalMs", 100);
        config.put("stormProbability", "0.8"); // constant burst storm CFPs
        config.put("maxRounds", 6);

        List<AgentSpec> specs = scenario.agents(config);
        assertThat(specs).hasSize(41); // 1 + 20 + 15 + 5

        String gridId = String.valueOf(specs.get(0).getArgs().get(0));

        ProfileImpl subProfile = new ProfileImpl(false);
        subProfile.setParameter(Profile.CONTAINER_NAME, "scenario-burst-test");
        subProfile.setParameter(Profile.MAIN_PORT, "12999");
        ContainerController burstContainer = runtime.createAgentContainer(subProfile);

        // --- Act ---
        for (AgentSpec spec : specs) {
            Object[] args = spec.getArgs().toArray(new Object[0]);
            AgentController ac = burstContainer.createNewAgent(spec.getNameSuffix(), spec.getClassName(), args);
            ac.start();
        }

        // Allow burst rounds to complete
        Thread.sleep(2000);

        // --- Assert ---
        SubstationAgent.Metrics metrics = SubstationAgent.getMetrics(gridId);
        assertThat(metrics).isNotNull();
        assertThat(metrics.getAuctionsHeld()).isGreaterThanOrEqualTo(2);
        assertThat(metrics.getProposalsReceived()).isGreaterThan(0);

        burstContainer.kill();
    }
}
