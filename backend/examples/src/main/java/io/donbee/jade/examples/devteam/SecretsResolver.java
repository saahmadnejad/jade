package io.donbee.jade.examples.devteam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.UnaryOperator;

/**
 * Resolves the LLM API key without ever reading it from the repository
 * (ADR-0002): environment variable first, then a gitignored local properties
 * file, then failure with an actionable message.
 */
public final class SecretsResolver {

    private static final io.donbee.jade.util.Logger LOG =
        io.donbee.jade.util.Logger.getJADELogger(SecretsResolver.class.getName());

    /*     * Gitignored file that may hold {@code llm.api.key=<secret>} (default profile). */
    public static final String DEFAULT_PROFILE = "local";

    private SecretsResolver() {
    }

    /** Name of the profile file: {@code secrets-<profile>.properties}. */
    static Path secretsFile(String profile) {
        return Path.of("conf", "secrets-" + profile + ".properties");
    }

    /**
     * Resolve the LLM API key from {@code NINEROUTER_API_KEY} env var first,
     * then the gitignored local file's {@code llm.api.key} property.
     *
     * @param envLookup    environment lookup (injectable for tests)
     * @param workingDir   directory containing {@code conf/secrets-local.properties}
     * @return the resolved API key
     * @throws IllegalStateException when no source provides a key
     */
    public static String resolveApiKey(UnaryOperator<String> envLookup,
                                       Path workingDir) {
        return resolve(envLookup, "NINEROUTER_API_KEY", workingDir, DEFAULT_PROFILE, "llm.api.key",
            "No LLM API key found. Set the 'NINEROUTER_API_KEY' environment variable ");
    }

    /**
     * Resolve the GitHub PAT: {@code GH_TOKEN} env var first, then the
     * gitignored local file's {@code github.token} property.
     */
    public static String resolveGithubToken(UnaryOperator<String> envLookup, Path workingDir) {
        return resolve(envLookup, "GH_TOKEN", workingDir, DEFAULT_PROFILE, "github.token",
            "No GitHub token found. Set the 'GH_TOKEN' environment variable ");
    }

    /**
     * Read an optional configuration property, checking environment variable
     * first then the local secrets file, falling back to {@code defaultValue}.
     * Unlike {@link #resolveApiKey}, this never throws — missing values use the
     * default (useful for non-secret settings like base URL or model name).
     */
    public static String resolveOptional(UnaryOperator<String> envLookup, String envVar,
                                         Path workingDir, String fileProperty, String defaultValue) {
        return resolveOptional(envLookup, envVar, workingDir, DEFAULT_PROFILE,
            fileProperty, defaultValue);
    }

    /**
     * Like {@link #resolveOptional}, but with an explicit secrets profile so
     * callers can read from {@code secrets-<profile>.properties} (e.g. a future
     * "shop" profile). Env var is still checked first.
     */
    public static String resolveOptional(UnaryOperator<String> envLookup, String envVar,
                                         Path workingDir, String profile,
                                         String fileProperty, String defaultValue) {
        String fromEnv = envLookup.apply(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = readFromFile(workingDir, profile, fileProperty);
        return fromFile != null && !fromFile.isBlank() ? fromFile.trim() : defaultValue;
    }

    private static String resolve(UnaryOperator<String> envLookup, String envVar,
                                  Path workingDir, String profile, String fileProperty, String errorPrefix) {
        String fromEnv = envLookup.apply(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = readFromFile(workingDir, profile, fileProperty);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        throw new IllegalStateException(
            errorPrefix
                + "or create " + workingDir.resolve(secretsFile(profile))
                + " (gitignored) with '" + fileProperty + "=<secret>'. Never commit secrets (ADR-0002).");
    }

    private static String readFromFile(Path workingDir, String profile, String property) {
        Path file = workingDir.resolve(secretsFile(profile));
        if (!Files.exists(file)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
            return props.getProperty(property);
        } catch (IOException e) {
            LOG.warning("Could not read " + file + ": " + e.getMessage());
            return null;
        }
    }
}
