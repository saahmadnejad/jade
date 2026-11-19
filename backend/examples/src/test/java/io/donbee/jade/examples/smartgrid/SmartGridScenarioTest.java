package io.donbee.jade.examples.smartgrid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.rest.scenario.ScenarioParam;

public class SmartGridScenarioTest {

    private final SmartGridScenario scenario = new SmartGridScenario();

    @Test
    public void Given_Scenario_When_MetadataQueried_Then_MatchesSpec() {
        // --- Arrange ---
        SmartGridScenario gridScenario = new SmartGridScenario();

        // --- Act ---
        String id = gridScenario.id();
        String title = gridScenario.title();
        String description = gridScenario.description();
        List<ScenarioParam> params = gridScenario.params();

        // --- Assert ---
        assertThat(id).isEqualTo("smart-grid");
        assertThat(title).isEqualTo("Smart Grid Microgrid & Decentralized Energy Trading");
        assertThat(description).contains("microgrid simulation");
        assertThat(params).extracting(ScenarioParam::getName)
            .containsExactly("prosumersCount", "consumersCount", "tickIntervalMs", "stormProbability");

        ScenarioParam prosumersParam = params.stream().filter(p -> p.getName().equals("prosumersCount")).findFirst().orElseThrow();
        assertThat(prosumersParam.getDefaultValue()).isEqualTo(100);
        assertThat(prosumersParam.getMaxValue()).isEqualTo(5000L);

        ScenarioParam consumersParam = params.stream().filter(p -> p.getName().equals("consumersCount")).findFirst().orElseThrow();
        assertThat(consumersParam.getDefaultValue()).isEqualTo(100);
        assertThat(consumersParam.getMaxValue()).isEqualTo(5000L);

        ScenarioParam stormParam = params.stream().filter(p -> p.getName().equals("stormProbability")).findFirst().orElseThrow();
        assertThat(stormParam.getDefaultValue()).isEqualTo("0.1");

        ScenarioParam tickParam = params.stream().filter(p -> p.getName().equals("tickIntervalMs")).findFirst().orElseThrow();
        assertThat(tickParam.getDefaultValue()).isEqualTo(500);
    }

    @Test
    public void Given_DefaultConfig_When_AgentsGenerated_Then_206AgentsGeneratedWithSolarAndWind() {
        // --- Arrange ---
        Map<String, Object> emptyConfig = Map.of();

        // --- Act ---
        List<AgentSpec> specs = scenario.agents(emptyConfig);

        // --- Assert ---
        // 1 substation + 100 prosumers + 100 consumers + 5 batteries = 206
        assertThat(specs).hasSize(206);
        assertThat(specs.get(0).getNameSuffix()).isEqualTo("substation");
        assertThat(specs.get(0).getClassName()).isEqualTo(SubstationAgent.class.getName());

        long prosumerCount = specs.stream().filter(s -> s.getNameSuffix().startsWith("prosumer-")).count();
        long consumerCount = specs.stream().filter(s -> s.getNameSuffix().startsWith("consumer-")).count();
        long batteryCount = specs.stream().filter(s -> s.getNameSuffix().startsWith("battery-")).count();

        assertThat(prosumerCount).isEqualTo(100);
        assertThat(consumerCount).isEqualTo(100);
        assertThat(batteryCount).isEqualTo(5);

        // Verify wind generation is modeled for subset of prosumers
        long windCount = specs.stream()
            .filter(s -> s.getNameSuffix().startsWith("prosumer-"))
            .filter(s -> Boolean.parseBoolean(String.valueOf(s.getArgs().get(4))))
            .count();
        assertThat(windCount).isEqualTo(33); // 100 / 3 = 33
    }

    @Test
    public void Given_CustomConfig_When_AgentsGenerated_Then_CountsMatchConfig() {
        // --- Arrange ---
        Map<String, Object> config = new HashMap<>();
        config.put("prosumersCount", 25);
        config.put("consumersCount", 15);
        config.put("stormProbability", "0.25");
        config.put("tickIntervalMs", 200);

        // --- Act ---
        List<AgentSpec> specs = scenario.agents(config);

        // --- Assert ---
        // 1 substation + 25 prosumers + 15 consumers + 1 battery (25/20) = 42
        assertThat(specs).hasSize(42);
        assertThat(specs.stream().filter(s -> s.getNameSuffix().startsWith("prosumer-")).count()).isEqualTo(25);
        assertThat(specs.stream().filter(s -> s.getNameSuffix().startsWith("consumer-")).count()).isEqualTo(15);
        assertThat(specs.stream().filter(s -> s.getNameSuffix().startsWith("battery-")).count()).isEqualTo(1);

        AgentSpec substation = specs.get(0);
        assertThat(substation.getArgs()).contains("200", "0.25");
    }

    @Test
    public void Given_LargeScaleConfiguration_When_1000AgentsGenerated_Then_SpecsAreValidAndNoOOM() {
        // --- Arrange ---
        Map<String, Object> config = new HashMap<>();
        config.put("prosumersCount", 500);
        config.put("consumersCount", 500);

        // --- Act ---
        List<AgentSpec> specs = scenario.agents(config);

        // --- Assert ---
        // 1 + 500 + 500 + 25 batteries = 1026 agents
        assertThat(specs).hasSize(1026);
    }
}
