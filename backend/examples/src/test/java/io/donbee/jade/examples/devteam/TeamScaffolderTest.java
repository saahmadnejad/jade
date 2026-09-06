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
}
