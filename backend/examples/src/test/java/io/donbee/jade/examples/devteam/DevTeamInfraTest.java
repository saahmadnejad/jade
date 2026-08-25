package io.donbee.jade.examples.devteam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Test;

public class DevTeamInfraTest {

    // ===== ArtifactParser =====

    @Test
    public void Given_LlmOutputWithFileBlocks_When_Parsed_Then_ArtifactsExtractedInOrder() {
        String llm = """
            Here is the plan explanation.

            ```python src/app.py
            print("hello")
            ```

            Some commentary between blocks.

            ```python tests/test_app.py
            def test_ok():
                assert True
            ```
            """;

        Map<String, String> artifacts = ArtifactParser.parse(llm);

        assertThat(artifacts).containsOnlyKeys("src/app.py", "tests/test_app.py");
        assertThat(artifacts.get("src/app.py")).isEqualTo("print(\"hello\")\n");
        assertThat(artifacts.get("tests/test_app.py")).contains("def test_ok():");
    }

    @Test
    public void Given_TextWithoutBlocks_When_Parsed_Then_EmptyResult() {
        assertThat(ArtifactParser.parse("just prose, no code blocks")).isEmpty();
        assertThat(ArtifactParser.parse(null)).isEmpty();
    }

    @Test
    public void Given_DuplicatePaths_When_Parsed_Then_LastBlockWins() {
        String llm = """
            ```python src/a.py
            v1 = 1
            ```
            ```python src/a.py
            v1 = 2
            ```
            """;
        assertThat(ArtifactParser.parse(llm).get("src/a.py")).contains("v1 = 2");
    }

    // ===== SecretsResolver (ADR-0002) =====

    @Test
    public void Given_EnvVarPresent_When_KeyResolved_Then_EnvWins() {
        String key = SecretsResolver.resolveApiKey(
            name -> "env-key", "MY_KEY", Path.of("nonexistent-dir"));
        assertThat(key).isEqualTo("env-key");
    }

    @Test
    public void Given_NoEnvButLocalFile_When_KeyResolved_Then_FileUsed() throws Exception {
        Path dir = Files.createTempDirectory("devteam-test");
        Path conf = dir.resolve("conf");
        Files.createDirectories(conf);
        Files.writeString(conf.resolve("secrets.local.properties"), "api.key=file-key\n");

        String key = SecretsResolver.resolveApiKey(name -> null, "MY_KEY", dir);
        assertThat(key).isEqualTo("file-key");
    }

    @Test
    public void Given_NeitherSource_When_KeyResolved_Then_ActionableFailure() throws Exception {
        Path dir = Files.createTempDirectory("devteam-empty").toAbsolutePath();
        assertThatThrownBy(() ->
            SecretsResolver.resolveApiKey(name -> null, "MY_KEY", dir))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MY_KEY")
            .hasMessageContaining("Never commit keys");
    }

    // ===== Workspace disk mirror =====

    @Test
    public void Given_MirrorDir_When_ArtifactsSaved_Then_FilesWrittenToDisk() throws Exception {
        Path mirror = Files.createTempDirectory("workspace-mirror");
        Workspace ws = new Workspace("t", mirror);

        ws.save("src/app.py", "print('hi')");
        ws.save("../escape.txt", "nope");

        assertThat(Files.readString(mirror.resolve("src/app.py"))).isEqualTo("print('hi')");
        assertThat(ws.snapshot()).containsOnlyKeys("src/app.py", "../escape.txt");
        assertThat(Files.exists(mirror.resolve("escape.txt"))).isFalse(); // traversal refused
    }
}
