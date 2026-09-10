package io.donbee.jade.examples.devteam;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import io.donbee.jade.core.AgentContainer;
import io.donbee.jade.core.AgentManager;
import io.donbee.jade.core.ProfileImpl;
import io.donbee.jade.core.Runtime;
import io.donbee.jade.rest.service.JadesPlatformService;
import io.donbee.jade.rest.service.PlatformService;
import io.donbee.jade.rest.service.ScenarioService;

/**
 * Offline FSM integration test: boots a real JADE platform, starts a dev-team
 * instance whose brains resolve to a stub {@code opencode} CLI on PATH, and
 * asserts the artifacts the Manager writes when the run finishes.
 *
 * <p>The stub rejects the first review round and approves the second, proving
 * the implement → test → review loop iterates. No network, no LLM: the stub
 * echoes canned per-role responses.</p>
 */
public class DevTeamFlowIntegrationTest {

    private static final String INSTANCE = "flowit";
    private static Runtime runtime;
    private static io.donbee.jade.wrapper.ContainerController wrapper;
    private static Path shimDir;
    private static Path workspaceDir;

    @BeforeClass
    public static void boot() throws Exception {
        Assume.assumeTrue("requires /bin/sh scripts",
            !System.getProperty("os.name").toLowerCase().contains("win"));

        workspaceDir = Files.createTempDirectory("devteam-flowit-ws");
        shimDir = Files.createTempDirectory("devteam-flowit-cli");
        writeShim();

        // CliBrain resolves the CLI via the 'opencode.cli' system property
        // (test seam; see CliBrain.resolveCli). Agents run in this JVM, and
        // env vars cannot be mutated post-start.
        System.setProperty("opencode.cli", shimDir.resolve("opencode").toString());

        runtime = Runtime.instance();
        ProfileImpl profile = new ProfileImpl(true);
        profile.setParameter(io.donbee.jade.core.Profile.MAIN, "true");
        // Scratch port to avoid clashing with a dev platform on 1099.
        profile.setParameter(io.donbee.jade.core.Profile.MAIN_PORT, "11999");
        wrapper = runtime.createMainContainer(profile);

        // Manager/agents need a moment for the container to come up.
        Thread.sleep(1500);
    }

    @AfterClass
    public static void shutdown() {
        if (wrapper != null) {
            try {
                wrapper.kill();
            } catch (Exception ignored) {
            }
        }
        if (runtime != null) {
            runtime.shutDown();
        }
    }

    @Test
    public void Given_StubBrains_When_InstanceRuns_Then_RoundLoopApprovesAndArtifactsLand() throws Exception {
        // --- Arrange: dev-team config with stub brains + temp workspace ---
        DevTeamScenario scenario = new DevTeamScenario();
        Map<String, Object> config = new HashMap<>();
        for (io.donbee.jade.rest.scenario.ScenarioParam p : scenario.params()) {
            config.put(p.getName(), p.getDefaultValue());
        }
        config.put("brief", "Build a tiny app");
        config.put("clarify", false);
        config.put("maxRounds", 3);
        config.put("workspaceDir", workspaceDir.toString());

        PlatformService platform = buildPlatformService();
        ScenarioService svc = new ScenarioService(platform);

        // --- Act: start instance (deploys 5 agents into scenario container) ---
        ScenarioService.StartResult result = svc.start("dev-team", INSTANCE, config);
        assertThat(result.agents).hasSize(5);

        // --- Assert: poll STATUS.md on the disk mirror until finished ---
        Path status = workspaceDir.resolve("STATUS.md");
        long deadline = System.currentTimeMillis() + 120_000;
        String content = null;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(status)) {
                content = Files.readString(status);
                if (content != null && content.contains("# Status:")) {
                    break;
                }
            }
            Thread.sleep(500);
        }
        assertThat(content).as("STATUS.md written within 120s").isNotNull();

        // Approve path: round1 rejected, round2 approved.
        assertThat(content).contains("APPROVED");
        assertThat(content).doesNotContain("FAILED");

        // Artifacts on disk (Manager writes via Workspace mirror; phase
        // prefixes: design artifacts under design/, code under src/,
        // reports under tests/ — saveArtifacts(text, prefix)).
        assertThat(Files.readString(workspaceDir.resolve("DESIGN.md"))).contains("# design");
        assertThat(Files.readString(workspaceDir.resolve("src/src/app.py"))).contains("print(1)");
        assertThat(Files.readString(workspaceDir.resolve("src/tests/test_app.py"))).contains("assert True");
        assertThat(Files.readString(workspaceDir.resolve("REVIEW-round1.md"))).contains("REJECTED");
        assertThat(Files.readString(workspaceDir.resolve("REVIEW-round2.md"))).contains("APPROVED");

        // The CLI stub saw the roles (persona wiring intact).
        String calls = Files.readString(shimDir.resolve("calls.log"));
        assertThat(calls).contains("architect");
        assertThat(calls).contains("implementer");
        assertThat(calls).contains("tester");
        assertThat(calls).contains("reviewer");

        // Memory-only mode (no githubOrg): no publish artifacts.
        assertThat(Files.exists(workspaceDir.resolve("GITHUB.md"))).isFalse();
    }

    /**
     * Stub opencode: role comes from --agent; reviewer rejects round 1 and
     * approves round 2 (marker file flips the verdict).
     */
    private static void writeShim() throws Exception {
        Path marker = workspaceDir.resolve(".reviewed");
        String script = """
            #!/bin/sh
            printf '%%s\\n' "$@" >> '%s'
            ROLE=""
            while [ $# -gt 0 ]; do
              case "$1" in
                --agent) ROLE="$2"; shift 2;;
                *) shift;;
              esac
            done
            case "$ROLE" in
              architect)
                printf 'DESIGN:\\n```md DESIGN.md\\n# design\\n```\\n'
                exit 0;;
              implementer)
                printf 'IMPL:\\n```python src/app.py\\nprint(1)\\n```\\n```python tests/test_app.py\\nassert True\\n```\\n'
                exit 0;;
              tester)
                printf 'TESTS PASSED:\\n```md TEST-REPORT-round1.md\\nall green\\n```\\n'
                exit 0;;
              reviewer)
                if [ -f '%s' ]; then
                  printf 'VERDICT: APPROVED'
                else
                  printf 'VERDICT: REJECTED - add tests'
                  touch '%s'
                fi
                exit 0;;
              *)
                printf 'stub: unknown role'; exit 1;;
            esac
            """.formatted(shimDir.resolve("calls.log"), marker, marker);
        Path opencode = shimDir.resolve("opencode");
        Files.writeString(opencode, script);
        opencode.toFile().setExecutable(true);
    }

    // ===== helpers mirroring RestAPIVerticle's reflection =====

    private static PlatformService buildPlatformService() throws Exception {
        Field f = io.donbee.jade.wrapper.ContainerController.class.getDeclaredField("myImpl");
        f.setAccessible(true);
        AgentContainer impl = (AgentContainer) f.get(wrapper);
        AgentManager agentManager = null;
        io.donbee.jade.core.MainContainer main = impl.getMain();
        if (main != null) {
            agentManager = (AgentManager) main;
        }
        return new JadesPlatformService(impl, agentManager);
    }
}
