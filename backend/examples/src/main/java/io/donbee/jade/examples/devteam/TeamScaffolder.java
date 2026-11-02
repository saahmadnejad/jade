package io.donbee.jade.examples.devteam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;


/**
 * Prepares the instance working directory so every Role Agent's workspace
 * is "skilled": vendors the bundled skills collection into
 * {@code .opencode/skills/}, writes one persona per role into
 * {@code .opencode/agent/} and an {@code AGENTS.md} with team conventions.
 */
public final class TeamScaffolder {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(TeamScaffolder.class.getName());

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
        scaffold(workDir, githubOrg, repoName, visibility, System::getenv);
    }

    /**
     * @param providerBaseUrl OpenAI-compatible base URL written into the
     *                        workspace's {@code opencode.json} provider block
     */
    public static void scaffold(Path workDir, String githubOrg, String repoName,
                                String visibility, String providerBaseUrl) throws IOException {
        scaffold(workDir, githubOrg, repoName, visibility, varName -> {
            if ("LLM_BASE_URL".equals(varName)) {
                return providerBaseUrl;
            }
            return System.getenv(varName);
        });
    }

    /**
     * Scaffold with an injectable environment lookup (for testing generic provider config
     * and deprecated alias resolution per ADR-0005).
     */
    public static void scaffold(Path workDir, String githubOrg, String repoName,
                                String visibility, java.util.function.UnaryOperator<String> envLookup) throws IOException {
        LOG.info("scaffolding workspace " + workDir);
        Files.createDirectories(workDir);
        copySkills(workDir.resolve(".opencode").resolve("skills"));
        for (String role : List.of("manager", "architect", "implementer", "tester", "reviewer")) {
            writePersona(workDir, role);
        }
        writeOpencodeJson(workDir, envLookup);
        writeAgentsMd(workDir, githubOrg, repoName, visibility);
    }

    /**
     * Provider config for the opencode CLI (ADR-0005): generic OpenAI-compatible
     * provider resolved from LLM_* environment variables. When no provider envs are
     * set, writes an empty provider skeleton so host/global opencode config and auth flow
     * through. The API key reference ({env:LLM_API_KEY} or deprecated alias
     * {env:TOKENROUTER_API_KEY}) is never stored in plain text on disk (ADR-0002).
     */
    private static void writeOpencodeJson(Path workDir, java.util.function.UnaryOperator<String> envLookup) throws IOException {
        String providerId = envLookup.apply("LLM_PROVIDER_ID");
        if (providerId == null || providerId.isBlank()) {
            providerId = "custom";
        } else {
            providerId = providerId.trim();
        }

        String baseUrl = envLookup.apply("LLM_BASE_URL");
        if (baseUrl != null) {
            baseUrl = baseUrl.trim();
        }

        String apiKey = envLookup.apply("LLM_API_KEY");
        String keyEnvVar = "LLM_API_KEY";
        if (apiKey == null || apiKey.isBlank()) {
            String tokenrouterKey = envLookup.apply("TOKENROUTER_API_KEY");
            if (tokenrouterKey != null && !tokenrouterKey.isBlank()) {
                LOG.warning("TOKENROUTER_API_KEY is deprecated; use LLM_API_KEY instead.");
                apiKey = tokenrouterKey.trim();
                keyEnvVar = "TOKENROUTER_API_KEY";
            }
        } else {
            apiKey = apiKey.trim();
        }

        boolean hasBaseUrl = baseUrl != null && !baseUrl.isBlank();
        boolean hasKey = apiKey != null && !apiKey.isBlank();

        String json;
        if (!hasBaseUrl && !hasKey) {
            json = """
                {
                  "$schema": "https://opencode.ai/config.json",
                  "providers": {},
                  "provider": {}
                }
                """;
        } else {
            String effectiveBaseUrl = hasBaseUrl ? baseUrl : "";
            json = """
                {
                  "$schema": "https://opencode.ai/config.json",
                  "providers": {
                    "%s": {
                      "name": "%s",
                      "package": "@opencode/ai/providers/openai-compatible",
                      "settings": {
                        "baseURL": "%s"
                      },
                      "headers": {
                        "Authorization": "Bearer {env:%s}"
                      }
                    }
                  },
                  "provider": {
                    "%s": {
                      "npm": "@ai-sdk/openai-compatible",
                      "name": "%s",
                      "options": {
                        "baseURL": "%s",
                        "apiKey": "{env:%s}"
                      }
                    }
                  }
                }
                """.formatted(
                    providerId, providerId, effectiveBaseUrl, keyEnvVar,
                    providerId, providerId, effectiveBaseUrl, keyEnvVar
                );
        }
        Files.writeString(workDir.resolve("opencode.json"), json);
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
        try {
            String p = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
            var url = TeamScaffolder.class.getResource(p);
            if (url == null) return false;
            if ("file".equalsIgnoreCase(url.getProtocol())) {
                return new java.io.File(url.toURI()).isDirectory();
            }
            if ("jar".equalsIgnoreCase(url.getProtocol())) {
                return url.getPath().endsWith("/") || !JarResources.list(resourcePath).isEmpty();
            }
            return url.getPath().endsWith("/");
        } catch (Exception e) {
            return false;
        }
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
        Path agentsFile = workDir.resolve(".opencode").resolve("agents").resolve(role + ".md");
        Path legacyAgentFile = workDir.resolve(".opencode").resolve("agent").resolve(role + ".md");
        Files.createDirectories(agentsFile.getParent());
        Files.createDirectories(legacyAgentFile.getParent());
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
        String frontmatter = """
            ---
            description: Dev-team %s persona
            mode: all
            ---

            """.formatted(role);
        String content = frontmatter + "## Persona\n\n" + prompt.trim() + "\n";
        Files.writeString(agentsFile, content);
        Files.writeString(legacyAgentFile, content);
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
