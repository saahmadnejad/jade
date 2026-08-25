package io.donbee.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

public class CliBrainTest {

    /** Shell script pretending to be a CLI brain. */
    private Path fakeCli(String body) throws Exception {
        Path script = Files.createTempFile("fake-cli-", ".sh");
        Files.writeString(script, "#!/bin/sh\n" + body + "\n");
        script.toFile().setExecutable(true);
        return script;
    }

    @Test
    public void Given_FakeCli_When_Respond_Then_ReturnsStdoutAndPassesPromptAndModel() throws Exception {
        // --- Arrange ---
        Path echoArgs = fakeCli("printf 'ARGS:%s' \"$*\"");
        CliBrain brain = new CliBrain(List.of("sh", echoArgs.toString()),
            "test/model", null, 10_000);

        // --- Act ---
        String out = brain.respond("Be brief.", "Say hi");

        // --- Assert ---
        assertThat(out).startsWith("ARGS:");
        assertThat(out)
            .contains("--model")
            .contains("test/model")
            .contains("Be brief.")
            .contains("---")
            .contains("Say hi");
    }

    @Test
    public void Given_NonZeroExit_When_Respond_Then_BrainExceptionWithStderr() throws Exception {
        // --- Arrange ---
        Path failer = fakeCli("echo 'boom detail' >&2; exit 3");
        CliBrain brain = new CliBrain(List.of("sh", failer.toString()), null, null, 10_000);

        // --- Act / Assert ---
        assertThatThrownBy(() -> brain.respond(null, "u"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("exited with code 3")
            .hasMessageContaining("boom detail");
    }

    @Test
    public void Given_Timeout_When_Respond_Then_BrainExceptionAndProcessKilled() throws Exception {
        // --- Arrange ---
        Path sleeper = fakeCli("sleep 30");
        CliBrain brain = new CliBrain(List.of("sh", sleeper.toString()), null, null, 300);

        // --- Act / Assert ---
        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> brain.respond(null, "u"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("timed out");
        assertThat(System.currentTimeMillis() - start).isLessThan(5_000);
    }

    @Test
    public void Given_EmptyOutput_When_Respond_Then_BrainException() throws Exception {
        // --- Arrange ---
        Path silent = fakeCli("exit 0");
        CliBrain brain = new CliBrain(List.of("sh", silent.toString()), null, null, 10_000);

        // --- Act / Assert ---
        assertThatThrownBy(() -> brain.respond(null, "u"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("no output");
    }
}
