package io.donbee.llm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Built-in {@link Brain.Tool} that runs bash commands locally.
 * Configured with a working directory so the LLM can create, edit, and run files.
 */
public class BashTool implements Brain.Tool {

    /** Hard cap on captured stdout/stderr so one runaway command cannot exhaust memory. */
    private static final int MAX_OUTPUT_CHARS = 20_000;

    private final Path workDir;
    private final Duration timeout;

    public BashTool(Path workDir, Duration timeout) {
        this.workDir = workDir != null ? workDir : Paths.get("").toAbsolutePath();
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(120);
    }

    @Override
    public String name() {
        return "bash";
    }

    @Override
    public String description() {
        return "Execute a bash command in the workspace directory. Use for file creation, "
            + "editing, running tests, git operations, and any shell command.";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("command", "string");
        params.put("timeout", "string");
        return params;
    }

    @Override
    public Map<String, Boolean> required() {
        return Map.of("command", true, "timeout", false);
    }

    @Override
    public String call(String argumentsJson) {
        String command = extract(argumentsJson, "command");
        if (command == null || command.isBlank()) {
            return "Error: 'command' parameter is required";
        }

        long seconds = this.timeout.getSeconds();
        String timeoutStr = extract(argumentsJson, "timeout");
        if (timeoutStr != null && !timeoutStr.isBlank()) {
            seconds = Math.min(parseTimeoutSeconds(timeoutStr), seconds);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(workDir.toFile());
            Process proc = pb.start();

            // Drain both streams concurrently so a full pipe never deadlocks the process.
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            Thread outDrain = drain(proc.getInputStream(), stdout);
            Thread errDrain = drain(proc.getErrorStream(), stderr);
            outDrain.join((seconds + 5) * 1000);
            errDrain.join((seconds + 5) * 1000);

            boolean finished = proc.waitFor(seconds, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                return "Error: command timed out after " + seconds + "s\n"
                    + truncate(stdout) + "\n" + truncate(stderr);
            }

            int exitCode = proc.exitValue();
            String result = "exit_code: " + exitCode + "\n";
            if (stdout.length() > 0) result += "stdout:\n" + truncate(stdout);
            if (stderr.length() > 0) result += "\nstderr:\n" + truncate(stderr);
            return result.trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static Thread drain(java.io.InputStream in, StringBuilder sink) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null && sink.length() < MAX_OUTPUT_CHARS) {
                    sink.append(line).append("\n");
                }
            } catch (Exception ignored) {
                // stream closed with the process
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static String truncate(StringBuilder sb) {
        return sb.length() >= MAX_OUTPUT_CHARS
            ? sb.substring(0, MAX_OUTPUT_CHARS) + "\n... (output truncated)"
            : sb.toString();
    }

    /**
     * Minimal JSON string extraction: finds the value of {@code "key": "..."}
     * with a hand-rolled scanner (linear scan, no regex backtracking), then
     * unescapes standard JSON escapes. Handles multi-line values and embedded
     * escaped quotes.
     */
    private static String extract(String json, String key) {
        String needle = "\"" + key + "\"";
        int k = json.indexOf(needle);
        while (k >= 0) {
            int i = k + needle.length();
            // skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i < json.length() && json.charAt(i) == ':') {
                i++;
                while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
                if (i < json.length() && json.charAt(i) == '"') {
                    return scanString(json, i + 1);
                }
            }
            k = json.indexOf(needle, k + 1);
        }
        return null;
    }

    /** Scans a JSON string body starting after the opening quote; returns null when unterminated. */
    private static String scanString(String json, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                if (i + 1 >= json.length()) {
                    return null;
                }
                // Keep the escape intact; unescape() interprets it below.
                sb.append(c).append(json.charAt(++i));
            } else if (c == '"') {
                return unescape(sb.toString());
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'u' -> {
                        if (i + 4 < s.length()) {
                            try {
                                sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u");
                            }
                        } else {
                            sb.append("\\u");
                        }
                    }
                    default -> sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static long parseTimeoutSeconds(String s) {
        try {
            return Duration.parse("PT" + s.toUpperCase()).getSeconds();
        } catch (Exception e) {
            try {
                return Long.parseLong(s.replaceAll("[^0-9].*$", "").trim());
            } catch (Exception e2) {
                return 120;
            }
        }
    }
}
