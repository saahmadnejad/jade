package io.donbee.jade.examples.devteam;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
 * REAL end-to-end assessment (gated): boots the platform and lets the five
 * agents run against the real opencode CLI + tokenrouter provider
 * ({@code tokenrouter/z-ai/glm-5.3-free}). Verifies the produced code, not
 * just the conversation: the generated library must compile, its tests must
 * pass, and an LLM judge scores the result (ADR-0003 quality gates).
 *
 * <p>Gating (skipped by default; costs real tokens, needs network):</p>
 * <ul>
 *   <li>{@code -Dit.real=true} opts in</li>
 *   <li>{@code TOKENROUTER_API_KEY} env must be set</li>
 * </ul>
 *
 * <p>Run: {@code TOKENROUTER_API_KEY=... mvn -pl examples -am test
 * -Dtest=DevTeamRealIT -Dit.real=true -Dsurefire.failIfNoSpecifiedTests=false}</p>
 */
public class DevTeamRealIT {

    private static final int STATUS_POLL_MS = 45 * 60 * 1000; // 45min cap
    private static io.donbee.jade.core.Runtime runtime;
    private static io.donbee.jade.wrapper.ContainerController wrapper;
    private static Path workspaceDir;

    @BeforeClass
    public static void gate() throws Exception {
        Assume.assumeTrue("opt-in: -Dit.real=true", Boolean.getBoolean("it.real"));
        Assume.assumeTrue("TOKENROUTER_API_KEY env required",
            System.getenv("TOKENROUTER_API_KEY") != null
                && !System.getenv("TOKENROUTER_API_KEY").isBlank());

        workspaceDir = Files.createTempDirectory("devteam-realit-ws");

        runtime = io.donbee.jade.core.Runtime.instance();
        ProfileImpl profile = new ProfileImpl(true);
        profile.setParameter(io.donbee.jade.core.Profile.MAIN, "true");
        profile.setParameter(io.donbee.jade.core.Profile.MAIN_PORT, "11998");
        wrapper = runtime.createMainContainer(profile);
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
    public void Given_RealBrains_When_TeamBuildsLibrary_Then_CodeCompilesTestsPassJudgeApproves() throws Exception {
        // --- Arrange: brief engineered to be tiny and objectively verifiable ---
        DevTeamScenario scenario = new DevTeamScenario();
        Map<String, Object> config = new HashMap<>();
        for (io.donbee.jade.rest.scenario.ScenarioParam p : scenario.params()) {
            config.put(p.getName(), p.getDefaultValue());
        }
        config.put("brief", """
            Build a tiny Python 3 library called `string_utils` (stdlib only):

            - `src/string_utils.py`: function `reverse(text: str) -> str` returning the
              reversed string, and function `is_palindrome(text: str) -> bool` that is
              case-insensitive and ignores spaces.
            - `tests/test_string_utils.py`: unittest-based tests for both functions,
              including edge cases (empty string, mixed case palindrome).

            Constraints: no third-party dependencies; the tests must be runnable with
              `python3 -m unittest discover -s tests` and exit 0.
            """);
        config.put("clarify", false);
        config.put("maxRounds", 3);
        config.put("workspaceDir", workspaceDir.toString());

        PlatformService platform = buildPlatformService();
        ScenarioService svc = new ScenarioService(platform);

        // --- Act: run the real team ---
        ScenarioService.StartResult result = svc.start("dev-team", "realit", config);
        assertThat(result.agents).hasSize(5);

        // Gate 1: terminal STATUS.
        Path status = workspaceDir.resolve("STATUS.md");
        String content = pollStatus(status);
        assertThat(content).as("STATUS.md").isNotNull();
        assertThat(content).as("terminal status, got: " + firstLines(content))
            .matches(s -> s.contains("APPROVED") || s.contains("PUBLISHED"));

        // Gate 2: produced library compiles.
        Path lib = findFile(workspaceDir, "string_utils.py");
        assertThat(lib).as("string_utils.py produced").isNotNull();
        assertThat(sh("python3 -m py_compile " + lib).exit).isEqualTo(0);

        // Gate 3: the team's own tests pass.
        Path tests = findFile(workspaceDir, "test_string_utils.py");
        assertThat(tests).as("test_string_utils.py produced").isNotNull();
        assertThat(sh("cd " + tests.getParent().getParent()
            + " && python3 -m unittest discover -s tests -t .").exit).isEqualTo(0);

        // Gate 4: LLM judge scores the artifact against the brief (>= 7/10).
        int score = judge(lib, tests);
        assertThat(score).as("LLM judge score (>=7 required)").isGreaterThanOrEqualTo(7);
    }

    // ===== helpers =====

    private record Sh(int exit, String out) {}

    private static Sh sh(String cmd) throws Exception {
        Process p = new ProcessBuilder("sh", "-c", cmd)
            .redirectErrorStream(true).start();
        StringBuilder out = new StringBuilder();
        try (var r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        p.waitFor();
        return new Sh(p.exitValue(), out.toString());
    }

    /** Recursively locate a file by name under dir. */
    private static Path findFile(Path dir, String name) {
        try (var walk = Files.walk(dir)) {
            return walk.filter(p -> p.getFileName().toString().equals(name))
                .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstLines(String s) {
        return s == null ? "" : s.substring(0, Math.min(80, s.length()));
    }

    /**
     * One extra opencode run as an LLM-as-judge: scores the produced library
     * 0-10 against the brief rubric; the last non-empty stdout line must be
     * {@code SCORE: <n>}.
     */
    private static int judge(Path lib, Path tests) throws Exception {
        String prompt = """
            You are an impartial judge. Score the following Python library 0-10
            against this rubric:
            - reverse() correct (3 pts)
            - is_palindrome() case-insensitive + ignores spaces (3 pts)
            - tests cover edge cases: empty string, mixed-case palindrome (2 pts)
            - stdlib only, tests runnable via unittest discover (2 pts)

            LIBRARY (%s):
            %s

            TESTS (%s):
            %s

            End your reply with EXACTLY one final line: SCORE: <number 0-10>
            """.formatted(lib, Files.readString(lib), tests, Files.readString(tests));

        Sh judgeRun = sh("cd " + workspaceDir
            + " && opencode run --auto -m tokenrouter/z-ai/glm-5.3-free " + shellQuote(prompt));
        assertThat(judgeRun.exit).as("judge opencode run").isZero();
        String lastLine = lastNonEmptyLine(judgeRun.out);
        assertThat(lastLine).as("judge verdict line, got: " + firstLines(judgeRun.out))
            .startsWith("SCORE:");
        return Integer.parseInt(lastLine.substring("SCORE:".length()).trim());
    }

    private static String lastNonEmptyLine(String s) {
        String[] lines = s.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].isBlank()) {
                return lines[i].trim();
            }
        }
        return "";
    }

    private static String shellQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    private static String pollStatus(Path status) throws Exception {
        long deadline = System.currentTimeMillis() + STATUS_POLL_MS;
        String content = null;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(status)) {
                content = Files.readString(status);
                if (content.contains("# Status:")) {
                    return content;
                }
            }
            Thread.sleep(2000);
        }
        return content;
    }

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
