package io.donbee.jade.examples.devteam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;


/**
 * Prepares the instance working directory so every Role Agent's opencode
 * session is "skilled": vendors the bundled mattpocock/skills collection into
 * {@code .opencode/skills/}, writes one persona per role into
 * {@code .opencode/agent/} and an {@code AGENTS.md} with team conventions.
 */
public final class TeamScaffolder {

    /** Resource root of the vendored skills inside the examples jar. */
    static final String SKILLS_RESOURCE_ROOT = "/skills";

    private TeamScaffolder() {
    }

    /**
     * @param workDir      the instance working directory (also CLI cwd)
     * @param githubOrg    target GitHub org, empty = memory-only mode
     * @param repoName     repo name under the org ({@code <teamId>-project})
     * @param visibility   "private" or "public"
     */
    public static void scaffold(Path workDir, String githubOrg, String repoName,
                                String visibility) throws IOException {
        Files.createDirectories(workDir);
        copySkills(workDir.resolve(".opencode").resolve("skills"));
        for (String role : List.of("manager", "architect", "implementer", "tester", "reviewer")) {
            writePersona(workDir, role);
        }
        writeAgentsMd(workDir, githubOrg, repoName, visibility);
    }

    private static void copySkills(Path target) throws IOException {
        if (Files.exists(target)) {
            return; // already scaffolded (e.g. same dir reused)
        }
        Files.createDirectories(target);
        List<String> names = resourceChildren(SKILLS_RESOURCE_ROOT);
        for (String category : names) {
            String categoryPath = SKILLS_RESOURCE_ROOT + "/" + category;
            if (!isDirectory(categoryPath)) {
                copyResourceFile(categoryPath, target.resolve(category));
                continue;
            }
            Path categoryDir = target.resolve(category);
            Files.createDirectories(categoryDir);
            for (String entry : resourceChildren(categoryPath)) {
                copyTree(categoryPath + "/" + entry, categoryDir.resolve(entry));
            }
        }
    }

    /** Recursively copy a classpath resource tree/file to disk. */
    private static void copyTree(String resourcePath, Path target) throws IOException {
        if (isDirectory(resourcePath)) {
            Files.createDirectories(target);
            for (String child : resourceChildren(resourcePath)) {
                copyTree(resourcePath + "/" + child, target.resolve(child));
            }
        } else {
            copyResourceFile(resourcePath, target);
        }
    }

    private static boolean isDirectory(String resourcePath) {
        // Directory resources end with '/' in getResource() URLs.
        var url = TeamScaffolder.class.getResource(resourcePath);
        return url != null && url.getPath().endsWith("/");
    }

    private static List<String> resourceChildren(String dirResource) throws IOException {
        // Works from exploded classes AND from inside a jar.
        List<String> names = new ArrayList<>(JarResources.list(dirResource));
        java.util.Collections.sort(names);
        return names;
    }

    private static void copyResourceFile(String resourcePath, Path target) throws IOException {
        try (InputStream in = TeamScaffolder.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writePersona(Path workDir, String role) throws IOException {
        Path agentFile = workDir.resolve(".opencode").resolve("agent").resolve(role + ".md");
        Files.createDirectories(agentFile.getParent());
        String prompt = switch (role) {
            case "manager" -> """
                You are the Manager of a software development team. You own the product
                intent: you translate the Brief into decisions and unblock teammates.
                Use the grilling and to-tickets skills when shaping requirements.""";
            case "architect" -> """
                You are the Architect. Before designing, clarify ambiguity using the
                grilling skill mindset: list your questions WITH the answer you assume
                for each. Then produce the design. Use codebase-design thinking.""";
            case "implementer" -> """
                You are the lead Implementer. Follow the implement and tdd skills:
                small increments, tests first where practical, complete files only.""";
            case "tester" -> """
                You are the quality engineer. Use the tdd skill conventions for test
                layout and write a concise test report.""";
            default -> """
                You are the Reviewer. Apply the code-review skill along two axes:
                standards and spec compliance. End with the verdict line contract
                defined by your task.""";
        };
        String frontmatter = "---\nname: %s\ndescription: Dev-team %s persona\n---\n\n"
            .formatted(role, role);
        Files.writeString(agentFile,
            frontmatter + "## Persona\n\n" + prompt.trim() + "\n");
    }

    private static void writeAgentsMd(Path workDir, String githubOrg, String repoName,
                                      String visibility) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Development team workspace\n\n")
          .append("This directory belongs to a virtual dev team. Your teammates exist; ")
          .append("a coordinator routes tasks between you as ACL messages. Do your job for ")
          .append("the task you were given, completely and autonomously.\n\n")
          .append("Skills are available via the skill tool - use the one matching your task.\n\n");
        if (githubOrg != null && !githubOrg.isBlank()) {
            sb.append("## GitHub\n\n")
              .append("- Organization: `").append(githubOrg).append("`\n")
              .append("- Repository: `").append(githubOrg).append("/").append(repoName).append("` (create it, ")
              .append(visibility).append(")\n")
              .append("`gh` and `git` are installed and authenticated via GH_TOKEN. When asked to ")
              .append("publish: init the repo here, commit all files, create the remote repository, ")
              .append("push, then report the repository URL.\n");
        }
        Files.writeString(workDir.resolve("AGENTS.md"), sb.toString());
    }
}
