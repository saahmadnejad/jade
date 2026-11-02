package io.donbee.jade.examples.devteam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class TeamScaffolderTest {

    @Test
    public void Given_ScaffoldCalled_When_WorkspacePrepared_Then_AgentsMdAndPersonasWritten() throws Exception {
        Path workDir = Files.createTempDirectory("team-scaffold-test");

        TeamScaffolder.scaffold(workDir, "", "test-project", "private");

        assertThat(Files.exists(workDir.resolve("AGENTS.md"))).isTrue();
        Path archPersona = workDir.resolve(".opencode").resolve("agent").resolve("architect.md");
        assertThat(Files.exists(archPersona)).isTrue();
        String persona = Files.readString(archPersona);
        assertThat(persona).contains("Architect");
    }

    @Test
    public void Given_ScaffoldCalled_When_GitHubOrgProvided_Then_AgentsMdHasGitHubSection() throws Exception {
        Path workDir = Files.createTempDirectory("team-scaffold-test2");

        TeamScaffolder.scaffold(workDir, "myorg", "repo-name", "private");

        String agentsMd = Files.readString(workDir.resolve("AGENTS.md"));
        assertThat(agentsMd).contains("Organization: `myorg`");
        assertThat(agentsMd).contains("Repository: `myorg/repo-name`");
    }

    @Test
    public void Given_ScaffoldCalled_When_SkillsCopied_Then_SkillsDirectoryPopulated() throws Exception {
        Path workDir = Files.createTempDirectory("team-scaffold-skills");

        TeamScaffolder.scaffold(workDir, "myorg", "repo-name", "private");

        Path skillsDir = workDir.resolve(".opencode").resolve("skills");
        assertThat(Files.exists(skillsDir)).isTrue();
        assertThat(Files.list(skillsDir).count()).isGreaterThan(0);
    }

    @Test
    public void Given_GenericLLMEnvs_When_Scaffold_Then_CustomProviderBlockWritten() throws Exception {
        Path workDir = Files.createTempDirectory("team-scaffold-generic");

        TeamScaffolder.scaffold(workDir, "", "generic-proj", "private", varName -> {
            return switch (varName) {
                case "LLM_PROVIDER_ID" -> "my-provider";
                case "LLM_BASE_URL" -> "https://my-llm.example.com/v1";
                case "LLM_API_KEY" -> "secret-123";
                default -> null;
            };
        });

        String config = Files.readString(workDir.resolve("opencode.json"));
        assertThat(config).contains("\"my-provider\"");
        assertThat(config).contains("https://my-llm.example.com/v1");
        assertThat(config).contains("{env:LLM_API_KEY}");
    }

    @Test
    public void Given_DeprecatedTokenrouterAlias_When_Scaffold_Then_TokenrouterAliasReferenceWritten() throws Exception {
        Path workDir = Files.createTempDirectory("team-scaffold-alias");

        TeamScaffolder.scaffold(workDir, "", "alias-proj", "private", varName -> {
            return switch (varName) {
                case "TOKENROUTER_API_KEY" -> "tr-secret-456";
                case "LLM_BASE_URL" -> "https://api.tokenrouter.com/v1";
                default -> null;
            };
        });

        String config = Files.readString(workDir.resolve("opencode.json"));
        assertThat(config).contains("\"custom\"");
        assertThat(config).contains("https://api.tokenrouter.com/v1");
        assertThat(config).contains("{env:TOKENROUTER_API_KEY}");
    }

    @Test
    public void Given_AllLLMEnvsUnset_When_Scaffold_Then_EmptyProviderSkeletonWritten() throws Exception {
        Path workDir = Files.createTempDirectory("team-scaffold-unset");

        TeamScaffolder.scaffold(workDir, "", "unset-proj", "private", varName -> null);

        String config = Files.readString(workDir.resolve("opencode.json"));
        assertThat(config).contains("\"providers\": {}");
    }
}
