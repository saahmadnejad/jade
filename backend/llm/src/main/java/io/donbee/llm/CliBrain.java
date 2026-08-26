package io.donbee.llm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * {@link Brain} backed by an external CLI tool (e.g. {@code opencode run},
 * {@code claude -p}). The configured command receives the combined prompts on
 * its argument list; stdout is the generated text.
 *
 * <p>Authentication is whatever the CLI tool itself is configured with — no
 * API keys flow through this library.</p>
 */
public class CliBrain implements Brain {

    private final List<String> command;
    private final String model;
    private final String agent;
    private final Path workingDir;
    private final int timeoutMs;

    /**
     * @param command    base command and arguments, e.g. {@code [opencode, run]}
     * @param model      optional model identifier forwarded as {@code --model <model>}
     *                   when non-null/non-blank
     * @param agent      optional opencode persona forwarded as {@code --agent <agent>}
     *                   when non-null/non-blank
     * @param workingDir working directory for the process (nullable)
     * @param timeoutMs  hard kill timeout for one invocation
     */
    public CliBrain(List<String> command, String model, String agent,
                    Path workingDir, int timeoutMs) {
        this.command = List.copyOf(Objects.requireNonNull(command, "command"));
        if (this.command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        this.model = model;
        this.agent = agent;
        this.workingDir = workingDir;
        this.timeoutMs = timeoutMs <= 0 ? 120_000 : timeoutMs;
    }

    @Override
    public String respond(String systemPrompt, String userPrompt) {
        List<String> cmd = new ArrayList<>(command);
        if (agent != null && !agent.isBlank()) {
            cmd.add("--agent");
            cmd.add(agent);
        }
        if (model != null && !model.isBlank()) {
            cmd.add("--model");
            cmd.add(model);
        }
        cmd.add(combine(systemPrompt, userPrompt));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }
        pb.redirectErrorStream(false);

        Process process = null;
        try {
            process = pb.start();
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BrainException("CLI brain '" + command.get(0) + "' timed out after "
                    + timeoutMs + " ms");
            }
            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());
            if (process.exitValue() != 0) {
                throw new BrainException("CLI brain '" + command.get(0) + "' exited with code "
                    + process.exitValue() + ": " + tail(stderr));
            }
            String response = stdout.trim();
            if (response.isEmpty()) {
                throw new BrainException("CLI brain '" + command.get(0)
                    + "' produced no output" + (stderr.isBlank() ? "" : "; stderr: " + tail(stderr)));
            }
            return response;
        } catch (IOException e) {
            throw new BrainException("Could not launch CLI brain '" + command.get(0)
                + "': " + e.getMessage() + " — is the tool installed and on PATH?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrainException("CLI brain interrupted", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static String combine(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return userPrompt;
        }
        return systemPrompt + "\n\n---\n\n" + userPrompt;
    }

    private static String tail(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        return trimmed.length() <= 500 ? trimmed : "..." + trimmed.substring(trimmed.length() - 500);
    }

    @Override
    public String model() {
        return model != null ? model : command.get(0);
    }
}
