package io.donbee.llm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Brain} that shells out to the opencode CLI agent. Instead of
 * calling an HTTP LLM endpoint, this implementation invokes the {@code opencode}
 * binary in a working directory, running its {@code run} subcommand with the
 * prompt as the message.
 *
 * <p>Useful when you want the full opencode agent inside a JADE role agent
 * (file system access, tool execution, multi-step reasoning). The opencode
 * CLI must be on PATH and authenticated (via 9router in this project).
 */
public class CliBrain implements Brain {

    private final String model;
    private final Path workDir;
    private final Duration timeout;

    public CliBrain(String model, Path workDir, Duration timeout) {
        this.model = model != null ? model : "combo-coding";
        this.workDir = workDir != null ? workDir : Paths.get("").toAbsolutePath();
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(300);
    }

    @Override
    public String respond(String systemPrompt, String userPrompt) {
        String fullPrompt = (systemPrompt != null && !systemPrompt.isBlank())
            ? systemPrompt + "\n\n---\n\n" + userPrompt
            : userPrompt;

        List<String> cmd = new ArrayList<>();
        cmd.add("opencode");
        cmd.add("run");
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

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = proc.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new BrainException("opencode CLI timed out after " + timeout.getSeconds() + "s");
            }

            int exitCode = proc.exitValue();
            if (exitCode != 0 && output.length() == 0) {
                throw new BrainException("opencode CLI exited with code " + exitCode);
            }
            return output.toString().trim();
        } catch (Exception e) {
            if (e instanceof BrainException) throw (BrainException) e;
            throw new BrainException("opencode CLI failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String respond(String systemPrompt, String userPrompt, List<Tool> tools) {
        return respond(systemPrompt, userPrompt);
    }
}
