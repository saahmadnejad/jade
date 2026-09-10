package io.donbee.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Brain} that shells out to the opencode CLI agent. Instead of
 * calling an HTTP LLM endpoint, this implementation invokes the {@code opencode}
 * binary in a working directory with its {@code run} subcommand, the prompt as
 * the message, and the role agent's persona loaded via {@code --agent}.
 *
 * <p>Useful when you want the full opencode agent inside a JADE role agent
 * (file system access, tool execution, multi-step reasoning). The opencode CLI
 * must be on PATH and its provider configured (see ADR-0003: an
 * {@code opencode.json} in the work directory defines the provider, with the
 * API key injected from the environment).</p>
 */
public class CliBrain implements Brain {

    /** Default opencode executable resolved from PATH. */
    public static final String DEFAULT_CLI = "opencode";

    private final String cliPath;
    private final String model;
    private final String role;
    private final Path workDir;
    private final Duration timeout;

    /**
     * @param model    opencode model id in {@code provider/model} form (e.g.
     *                 {@code tokenrouter/z-ai/glm-5.3-free}); nullable
     * @param role     agent persona name for {@code --agent} (e.g. the JADE
     *                 role: architect, implementer, ...); nullable/blank omits the flag
     * @param workDir  directory the CLI runs in (persona + opencode.json live
     *                 there); nullable defaults to cwd
     * @param timeout  per-call process timeout; nullable defaults to 600s
     * @param cliPath  executable to invoke; nullable resolves from the
     *                 {@code OPENCODE_CLI} env var then {@link #DEFAULT_CLI}
     *                 (tests point OPENCODE_CLI at a stub script)
     */
    public CliBrain(String model, String role, Path workDir, Duration timeout, String cliPath) {
        this.model = model;
        this.role = role;
        this.workDir = workDir != null ? workDir : Paths.get("").toAbsolutePath();
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(600);
        this.cliPath = resolveCli(cliPath);
    }

    private static String resolveCli(String cliPath) {
        if (cliPath != null && !cliPath.isBlank()) {
            return cliPath;
        }
        // Test seam: a mutable system property (env vars cannot be changed
        // after JVM start, which in-process tests need).
        String fromProp = System.getProperty("opencode.cli");
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp;
        }
        String fromEnv = System.getenv("OPENCODE_CLI");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return DEFAULT_CLI;
    }

    @Override
    public String respond(String systemPrompt, String userPrompt) {
        String fullPrompt = (systemPrompt != null && !systemPrompt.isBlank())
            ? systemPrompt + "\n\n---\n\n" + userPrompt
            : userPrompt;

        List<String> cmd = new ArrayList<>();
        cmd.add(cliPath);
        cmd.add("run");
        cmd.add("--auto");
        if (role != null && !role.isBlank()) {
            cmd.add("--agent");
            cmd.add(role);
        }
        if (model != null && !model.isBlank()) {
            cmd.add("-m");
            cmd.add(model);
        }
        cmd.add(fullPrompt);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // Gobble output on a separate thread: reading synchronously
            // would block past the timeout when the CLI is hung and silent.
            StringBuilder output = new StringBuilder();
            Thread gobbler = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (output) {
                            output.append(line).append("\n");
                        }
                    }
                } catch (IOException ignored) {
                    // process died mid-read; exit code decides
                }
            });

            boolean finished = proc.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                gobbler.interrupt();
                throw new BrainException("opencode CLI timed out after " + timeout.getSeconds() + "s");
            }
            gobbler.join(5000);

            int exitCode = proc.exitValue();
            String text;
            synchronized (output) {
                text = output.toString().trim();
            }
            if (exitCode != 0 || text.isEmpty()) {
                throw new BrainException("opencode CLI failed (exit " + exitCode
                    + ", output " + output.length() + " chars): "
                    + truncate(text, 200));
            }
            return text;
        } catch (BrainException e) {
            throw e;
        } catch (Exception e) {
            throw new BrainException("opencode CLI failed: " + e.getMessage(), e);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    @Override
    public String model() {
        return model != null ? model : DEFAULT_CLI;
    }

    @Override
    public String respond(String systemPrompt, String userPrompt, List<Tool> tools) {
        return respond(systemPrompt, userPrompt);
    }

    /** Work directory the CLI runs in (exposed for tests). */
    Path workDir() {
        return workDir;
    }

    /** True when the CLI binary is invokable (at least exists on PATH). */
    public boolean cliAvailable() {
        if (!cliPath.equals(DEFAULT_CLI)) {
            return Files.isExecutable(Paths.get(cliPath));
        }
        for (String dir : System.getenv("PATH").split(java.io.File.pathSeparator)) {
            if (!dir.isBlank() && Files.isExecutable(Paths.get(dir, DEFAULT_CLI))) {
                return true;
            }
        }
        return false;
    }
}
