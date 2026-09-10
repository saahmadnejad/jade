package io.donbee.jade.rest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import io.donbee.jade.rest.scenario.AgentSpec;
import io.donbee.jade.rest.scenario.Scenario;
import io.donbee.jade.rest.scenario.ScenarioParam;

public class ScenarioServiceTest {

    private PlatformService mockPlatform;
    private ScenarioService service;

    private final Scenario dummyScenario = new Scenario() {
        @Override
        public String id() {
            return "dummy";
        }

        @Override
        public String title() {
            return "Dummy Scenario";
        }

        @Override
        public String description() {
            return "demo";
        }

        @Override
        public List<ScenarioParam> params() {
            return List.of(
                ScenarioParam.intParam("stock", 10, 0, 100, "stock"),
                ScenarioParam.intParam("customers", 1, 1, 5, "customers"));
        }

        @Override
        public List<AgentSpec> agents(Map<String, Object> config) {
            int customers = (Integer) config.get("customers");
            List<AgentSpec> specs = new java.util.ArrayList<>();
            specs.add(new AgentSpec("store", "com.example.Store", List.of()));
            for (int i = 1; i <= customers; i++) {
                specs.add(new AgentSpec("customer" + i, "com.example.Customer", List.of(String.valueOf(i))));
            }
            return specs;
        }
    };

    @Before
    public void setUp() {
        mockPlatform = mock(PlatformService.class);
        service = newService(Map.of("dummy", dummyScenario));
    }

    /** Stub out real container creation: pretend each container joins fine. */
    private ScenarioService newService(Map<String, Scenario> scenarios) {
        return new ScenarioService(mockPlatform, scenarios) {
            @Override
            protected String createScenarioContainer(String instanceName) {
                return "scenario-" + instanceName;
            }
        };
    }

    @Test
    public void Given_ScenarioOnClasspath_When_StartWithDefaults_Then_AllAgentsDeployedIntoScenarioContainer() {
        // --- Act ---
        ScenarioService.StartResult result = service.start("dummy", null, null);

        // --- Assert ---
        assertThat(result.instance).isEqualTo("dummy-1");
        assertThat(result.container).isEqualTo("scenario-dummy-1");
        assertThat(result.agents).containsExactly("dummy-1-store", "dummy-1-customer1");
        verify(mockPlatform).deployAgent(eq("dummy-1-store"), eq("com.example.Store"), any(), eq("scenario-dummy-1"));
    }

    @Test
    public void Given_ExplicitConfig_When_Start_Then_ConfigPassedToScenarioAndInstanceNameHonoured() {
        // --- Arrange ---

        // --- Act ---
        ScenarioService.StartResult result = service.start("dummy", "demo", Map.of("customers", 2));

        // --- Assert ---
        assertThat(result.instance).isEqualTo("demo");
        assertThat(result.agents).hasSize(3); // store + customer1 + customer2
        verify(mockPlatform).deployAgent(eq("demo-customer2"), eq("com.example.Customer"), eq(new Object[]{"2"}), eq("scenario-demo"));
    }

    @Test
    public void Given_SecondInstanceOfSameScenario_When_Start_Then_BothTrackedIndependently() {
        // --- Act ---
        service.start("dummy", "a", null);
        service.start("dummy", "b", null);

        // --- Assert ---
        assertThat(service.listInstances()).hasSize(2);
        assertThat(service.listInstances()).extracting(ScenarioService.InstanceInfo::instance)
            .containsExactlyInAnyOrder("a", "b");
    }

    @Test
    public void Given_InvalidConfigValue_When_Start_Then_RejectedWithoutDeployingAnything() {
        // --- Act / Assert ---
        assertThatThrownBy(() -> service.start("dummy", "x", Map.of("stock", 500)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'stock' must be <=");
        verify(mockPlatform, never()).deployAgent(any(), any(), any(), any());
    }

    @Test
    public void Given_UnknownConfigKey_When_Start_Then_BadRequest() {
        // --- Act / Assert ---
        assertThatThrownBy(() -> service.start("dummy", "x", Map.of("nonsense", 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown config parameter");
    }

    @Test
    public void Given_BadInstanceName_When_Start_Then_Rejected() {
        // --- Act / Assert ---
        assertThatThrownBy(() -> service.start("dummy", "bad name!", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid instance name");
    }

    @Test
    public void Given_DuplicateInstanceName_When_Start_Then_Conflict() {
        // --- Arrange ---
        service.start("dummy", "dup", null);

        // --- Act / Assert ---
        assertThatThrownBy(() -> service.start("dummy", "dup", null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    public void Given_RunningInstance_When_Stop_Then_ContainerKilledAndTrackingRemoved() {
        // --- Arrange ---
        service.start("dummy", "killme", null);

        // --- Act ---
        service.stop("killme");

        // --- Assert ---
        verify(mockPlatform).killContainer("scenario-killme");
        assertThat(service.listInstances()).isEmpty();
    }

    @Test
    public void Given_ContainerCreationFails_When_Start_Then_AgentsFallBackToMainContainer() {
        // --- Arrange ---
        ScenarioService svc = new ScenarioService(mockPlatform, Map.of("dummy", dummyScenario)) {
            @Override
            protected String createScenarioContainer(String instanceName) {
                throw new RuntimeException("no platform connection");
            }
        };
        when(mockPlatform.getPlatformInfo()).thenReturn(
            new PlatformService.PlatformInfo("id", "Main-Container", true, "ams", "df"));

        // --- Act ---
        ScenarioService.StartResult result = svc.start("dummy", "fallback", null);

        // --- Assert ---
        assertThat(result.container).isEqualTo("Main-Container");
        verify(mockPlatform).deployAgent(eq("fallback-store"), eq("com.example.Store"), any(), eq((String) null));
    }

    @Test
    public void Given_ContainerKilledExternally_When_InstancesListed_Then_StaleInstancePruned() {
        // --- Arrange ---
        when(mockPlatform.getPlatformInfo()).thenReturn(new PlatformService.PlatformInfo("id", "Main-Container", true, "ams", "df"));
        service.start("dummy", "ghost", null);
        assertThat(service.listInstances()).extracting(ScenarioService.InstanceInfo::instance)
            .containsExactly("ghost");

        // Container killed outside the scenarios API (e.g. Containers page):
        when(mockPlatform.getContainers()).thenReturn(List.of(
            new PlatformService.ContainerInfo("Main-Container", "h", "1", true)));
        when(mockPlatform.getAgents(true)).thenReturn(List.of());

        // --- Act ---
        List<ScenarioService.InstanceInfo> remaining = service.listInstances();

        // --- Assert ---
        assertThat(remaining).isEmpty();
    }

    @Test
    public void Given_FallbackInstanceWithAllAgentsDead_When_InstancesListed_Then_Pruned() {
        // --- Arrange ---
        ScenarioService fallbackService = new ScenarioService(mockPlatform, Map.of("dummy", dummyScenario)) {
            @Override
            protected String createScenarioContainer(String instanceName) {
                throw new RuntimeException("no platform connection");
            }
        };
        when(mockPlatform.getPlatformInfo()).thenReturn(new PlatformService.PlatformInfo("id", "Main-Container", true, "ams", "df"));
        fallbackService.start("dummy", "fb", null);

        // All agents gone, but the Main Container is still alive:
        when(mockPlatform.getContainers()).thenReturn(List.of(
            new PlatformService.ContainerInfo("Main-Container", "h", "1", true)));
        when(mockPlatform.getAgents(true)).thenReturn(List.of(
            new PlatformService.AgentInfo("someone-else", "ACTIVE", "", "Main-Container", new String[]{})));

        // --- Act ---
        List<ScenarioService.InstanceInfo> remaining = fallbackService.listInstances();

        // --- Assert ---
        assertThat(remaining).isEmpty();
    }

    @Test
    public void Given_LiveInstanceAndExternalKillOfOther_When_InstancesListed_Then_LiveSurvives() {
        // --- Arrange ---
        when(mockPlatform.getPlatformInfo()).thenReturn(new PlatformService.PlatformInfo("id", "Main-Container", true, "ams", "df"));
        service.start("dummy", "alive", null);
        when(mockPlatform.getContainers()).thenReturn(List.of(
            new PlatformService.ContainerInfo("Main-Container", "h", "1", true),
            new PlatformService.ContainerInfo("scenario-alive", "h", "2", false)));
        when(mockPlatform.getAgents(true)).thenReturn(List.of());

        // --- Act ---
        List<ScenarioService.InstanceInfo> remaining = service.listInstances();

        // --- Assert ---
        assertThat(remaining).extracting(ScenarioService.InstanceInfo::instance)
            .containsExactly("alive");
    }

    @Test
    public void Given_DeploymentFailsMidway_When_Start_Then_AlreadyDeployedAgentsRolledBack() {
        // --- Arrange ---
        doThrow(new RuntimeException("boom"))
            .when(mockPlatform)
            .deployAgent(eq("r-agent"), eq("com.example.C"), any(), eq("scenario-r"));

        Scenario failing = new Scenario() {
            @Override public String id() { return "failing"; }
            @Override public String title() { return "f"; }
            @Override public String description() { return ""; }
            @Override public List<ScenarioParam> params() { return List.of(); }
            @Override public List<AgentSpec> agents(Map<String, Object> config) {
                return List.of(
                    new AgentSpec("first", "com.example.A", List.of()),
                    new AgentSpec("agent", "com.example.C", List.of()));
            }
        };
        ScenarioService svc = newService(Map.of("failing", failing));
        svc.start("failing", "seed", null); // not needed; use fresh name below

        // --- Act / Assert ---
        assertThatThrownBy(() -> svc.start("failing", "r", null))
            .isInstanceOf(RuntimeException.class);
        verify(mockPlatform).killAgent("r-first"); // rolled back
        assertThat(svc.listInstances()).extracting(ScenarioService.InstanceInfo::instance)
            .containsExactly("seed");
    }

    @Test
    public void Given_UnknownScenarioOrInstance_When_Accessed_Then_ErrorsThrown() {
        // --- Act / Assert ---
        assertThatThrownBy(() -> service.start("nope", "i", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown scenario");
        assertThatThrownBy(() -> service.stop("ghost"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown instance");
    }
}
