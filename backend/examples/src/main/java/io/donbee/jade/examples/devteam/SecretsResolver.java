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

    /** Gitignored file that may hold `api.key=<secret>`. */
    static final Path LOCAL_SECRETS_FILE =
        Path.of("conf", "secrets.local.properties");

    private SecretsResolver() {
    }

    /**
     * @param envLookup    environment lookup (injectable for tests)
     * @param keyEnvVar    environment variable name holding the API key
     * @param workingDir   directory containing {@code conf/secrets.local.properties}
     * @return the resolved API key
     * @throws IllegalStateException when no source provides a key
     */
    public static String resolveApiKey(UnaryOperator<String> envLookup,
                                       String keyEnvVar,
                                       Path workingDir) {
        return resolve(envLookup, keyEnvVar, workingDir, "api.key",
            "No LLM API key found. Set the '" + keyEnvVar + "' environment variable ");
    }

    /**
     * Resolve the GitHub PAT: {@code GH_TOKEN} env var first, then the
     * gitignored local file's {@code github.token} property.
     */
    public static String resolveGithubToken(UnaryOperator<String> envLookup, Path workingDir) {
        return resolve(envLookup, "GH_TOKEN", workingDir, "github.token",
            "No GitHub token found. Set the 'GH_TOKEN' environment variable ");
    }

    private static String resolve(UnaryOperator<String> envLookup, String envVar,
                                  Path workingDir, String fileProperty, String errorPrefix) {
        String fromEnv = envLookup.apply(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = readFromLocalFile(workingDir, fileProperty);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        throw new IllegalStateException(
            errorPrefix
                + "or create " + workingDir.resolve(LOCAL_SECRETS_FILE)
                + " (gitignored) with '" + fileProperty + "=<secret>'. Never commit secrets (ADR-0002).");
    }

    private static String readFromLocalFile(Path workingDir, String property) {
        Path file = workingDir.resolve(LOCAL_SECRETS_FILE);
        if (!Files.exists(file)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
            return props.getProperty(property);
        } catch (IOException e) {
            System.err.println("[SecretsResolver] Could not read " + file + ": " + e.getMessage());
            return null;
        }
    }
}
