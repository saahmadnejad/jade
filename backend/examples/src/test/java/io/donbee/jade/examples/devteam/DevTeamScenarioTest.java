package io.donbee.jade.examples.devteam;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.donbee.jade.rest.scenario.AgentSpec;

import org.junit.Test;

public class DevTeamScenarioTest {

    private final DevTeamScenario scenario = new DevTeamScenario();

    /** Complete config from declared defaults plus overrides. */
    private Map<String, Object> config(Object... overrides) {
        Map<String, Object> result = new java.util.HashMap<>();
        for (io.donbee.jade.rest.scenario.ScenarioParam p : scenario.params()) {
            result.put(p.getName(), p.getDefaultValue());
        }
        for (int i = 0; i < overrides.length; i += 2) {
            result.put((String) overrides[i], overrides[i + 1]);
        }
        return result;
    }

    @Test
    public void Given_Defaults_When_AgentsComputed_Then_ManagerPlusFourRoles() {
        var specs = scenario.agents(config());

        assertThat(specs).extracting(AgentSpec::getNameSuffix)
            .containsExactly("manager", "architect", "implementer", "tester", "reviewer");
    }

    @Test
    public void Given_Defaults_When_AgentsComputed_Then_CliBrainIsDefault() {
        var specs = scenario.agents(config("workspaceDir", "/tmp/team-ws"));

        AgentSpec architect = specs.get(1);
        assertThat(architect.getArgs().get(0)).isEqualTo("cli");                 // brainType
        assertThat(architect.getArgs()).contains("opencode run --auto", "/tmp/team-ws");
        assertThat(architect.getArgs().get(2)).isEqualTo("thinkingmachines/inkling:free");
        assertThat(architect.getArgs().get(3)).isEqualTo("ox-alpha"); // fallback model
    }

    @Test
    public void Given_RoleOverrides_When_AgentsComputed_Then_BrainArgsCarryModelAndProvider() {
        var specs = scenario.agents(config(
            "brainType", "http",
            "implementerModel", "some/paid-model",
            "fallbackModel", "backup/model",
            "proxyEnabled", false,
            "proxyPort", 9999,
            "workspaceDir", "/tmp/team-ws"));

        AgentSpec implementer = specs.get(2);
        assertThat(implementer.getClassName()).isEqualTo(DevTeamScenario.IMPLEMENTER_CLASS);
        assertThat(implementer.getArgs()).containsExactly(
            "http", "https://openrouter.ai/api/v1", "some/paid-model", "backup/model",
            "false", "192.168.1.151", "9999", "300", "OPENROUTER_API_KEY",
            "opencode run --auto", "/tmp/team-ws");
    }

    @Test
    public void Given_ManagerSpec_When_AgentsComputed_Then_CapsAndWorkspacePassedThrough() {
        var specs = scenario.agents(config("maxRounds", 5, "maxTotalCalls", 30,
            "brief", "Build X", "workspaceDir", "/tmp/team-ws"));

        AgentSpec manager = specs.get(0);
        assertThat(manager.getArgs()).containsExactly(
            "Build X", "5", "30", "5", "/tmp/team-ws", "moreshco-agents", "private"); // 300s -> 5min
    }
}
