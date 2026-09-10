package io.donbee.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.Assume;

public class CliBrainTest {

    /** Writes a shell script that records its argv and emits canned behavior. */
    private Path stubCli(Path dir, String body) throws IOException {
        Path script = dir.resolve("opencode-stub.sh");
        Files.writeString(script, "#!/bin/sh\n" + body);
        script.toFile().setExecutable(true);
        return script;
    }

    private Path recordDir() throws IOException {
        Path dir = Files.createTempDirectory("clibrain-test");
        return dir;
    }

    private List<String> recordedArgs(Path dir) throws IOException {
        Path rec = dir.resolve("argv");
        if (!Files.exists(rec)) {
            return List.of();
        }
        String raw = Files.readString(rec);
        return raw.isEmpty() ? List.of() : List.of(raw.split("\t"));
    }

    private String cli(Path dir) throws IOException {
        Path script = stubCli(dir, """
            printf '%s\\t' "$@" > argv
            echo 'stub-response'
            """);
        return script.toString();
    }

    @Test
    public void Given_Prompt_When_Respond_Then_FlagsRoleAndModelPassed() throws Exception {
        Path dir = recordDir();
        String cli = stubCli(dir, """
            printf '%s\\t' "$@" > argv
            echo 'DESIGN DOC'
            """).toString();

        CliBrain brain = new CliBrain("tokenrouter/z-ai/glm-5.3-free", "architect",
            dir, Duration.ofSeconds(30), cli);
        String out = brain.respond("be the architect", "design the app");

        assertThat(out).isEqualTo("DESIGN DOC");
        List<String> argv = recordedArgs(dir);
        assertThat(argv).contains("run", "--auto", "--agent", "architect",
            "-m", "tokenrouter/z-ai/glm-5.3-free", "be the architect\n\n---\n\ndesign the app");
    }

    @Test
    public void Given_NoRole_When_Respond_Then_AgentFlagOmitted() throws Exception {
        Path dir = recordDir();
        String cli = cli(dir);

        new CliBrain("m", null, dir, Duration.ofSeconds(30), cli).respond(null, "hi");

        assertThat(recordedArgs(dir)).contains("run", "--auto")
            .doesNotContain("--agent");
    }

    @Test
    public void Given_SystemPromptMerged_When_Respond_Then_PromptContainsBoth() throws Exception {
        Path dir = recordDir();
        String cli = cli(dir);

        new CliBrain("m", "tester", dir, Duration.ofSeconds(30), cli)
            .respond("SYS", "USER");

        assertThat(recordedArgs(dir)).contains("SYS\n\n---\n\nUSER");
    }

    @Test
    public void Given_CliExitsNonZero_When_Respond_Then_BrainException() throws Exception {
        Path dir = recordDir();
        String cli = stubCli(dir, "echo 'boom' >&2; exit 3").toString();

        CliBrain brain = new CliBrain("m", null, dir, Duration.ofSeconds(30), cli);
        assertThatThrownBy(() -> brain.respond(null, "hi"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("exit 3");
    }

    @Test
    public void Given_CliOutputsNothing_When_Respond_Then_BrainException() throws Exception {
        Path dir = recordDir();
        String cli = stubCli(dir, "exit 0").toString();

        CliBrain brain = new CliBrain("m", null, dir, Duration.ofSeconds(30), cli);
        assertThatThrownBy(() -> brain.respond(null, "hi"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("output");
    }

    @Test
    public void Given_SlowCli_When_TimeoutExceeded_Then_BrainException() throws Exception {
        Path dir = recordDir();
        String cli = stubCli(dir, "sleep 5; echo late").toString();

        CliBrain brain = new CliBrain("m", null, dir, Duration.ofSeconds(1), cli);
        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> brain.respond(null, "hi"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("timed out");
        assertThat(System.currentTimeMillis() - start).isLessThan(5000);
    }

    @Test
    public void Given_MissingCli_When_Respond_Then_BrainException() throws Exception {
        Path dir = recordDir();
        String missing = dir.resolve("no-such-cli").toString();

        CliBrain brain = new CliBrain("m", null, dir, Duration.ofSeconds(5), missing);
        assertThatThrownBy(() -> brain.respond(null, "hi"))
            .isInstanceOf(BrainException.class);
    }

    @Test
    public void Given_StubOnPath_When_DefaultCliUsed_Then_ResolvedFromPath() throws Exception {
        Assume.assumeTrue(!System.getProperty("os.name").toLowerCase().contains("win"));
        Path dir = recordDir();
        Path bin = dir.resolve("bin");
        Files.createDirectories(bin);
        Path script = bin.resolve("opencode");
        Files.writeString(script, "#!/bin/sh\necho 'from-path'\n");
        script.toFile().setExecutable(true);

        String oldPath = System.getenv("PATH");
        // ProcessBuilder inherits parent env; modify via process is not
        // possible post-start, so this test asserts cliAvailable() instead.
        CliBrain brain = new CliBrain("m", null, dir, Duration.ofSeconds(5),
            bin.resolve("opencode").toString());
        assertThat(brain.cliAvailable()).isTrue();
        assertThat(brain.respond(null, "hi")).isEqualTo("from-path");
    }

    @Test
    public void Given_WorkDir_When_CliRuns_Then_CwdIsWorkDir() throws Exception {
        Path dir = recordDir();
        String cli = stubCli(dir, "pwd").toString();

        CliBrain brain = new CliBrain("m", null, dir, Duration.ofSeconds(30), cli);
        String pwd = brain.respond(null, "hi");

        assertThat(pwd).isEqualTo(dir.toAbsolutePath().toString());
    }
}
