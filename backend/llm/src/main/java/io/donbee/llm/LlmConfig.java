package io.donbee.llm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Connection settings for {@link HttpBrain}. The API key is supplied lazily
 * via a supplier so secrets can live in the environment or a local file
 * instead of configuration objects (see ADR-0002).
 */
public final class LlmConfig {

    private final String baseUrl;
    private final String model;
    private final Supplier<String> apiKeySupplier;
    private final String proxyHost;
    private final Integer proxyPort;
    private final int timeoutMs;

    private LlmConfig(Builder b) {
        this.baseUrl = Objects.requireNonNull(b.baseUrl, "baseUrl");
        this.model = Objects.requireNonNull(b.model, "model");
        this.apiKeySupplier = Objects.requireNonNull(b.apiKeySupplier, "apiKeySupplier");
        this.proxyHost = b.proxyHost;
        this.proxyPort = b.proxyPort;
        this.timeoutMs = b.timeoutMs <= 0 ? 60_000 : b.timeoutMs;
        if (proxyPort != null && proxyHost == null) {
            throw new IllegalArgumentException("proxyPort set but proxyHost is missing");
        }
    }

    public static Builder builder(String baseUrl, String model) {
        return new Builder(baseUrl, model);
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String model() {
        return model;
    }

    public Supplier<String> apiKeySupplier() {
        return apiKeySupplier;
    }

    /** SOCKS5 proxy host, or null for direct connections. */
    public String proxyHost() {
        return proxyHost;
    }

    public Integer proxyPort() {
        return proxyPort;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public boolean useProxy() {
        return proxyHost != null && proxyPort != null && proxyPort > 0;
    }

    public static final class Builder {
        private final String baseUrl;
        private final String model;
        private Supplier<String> apiKeySupplier = () -> null;
        private String proxyHost;
        private Integer proxyPort;
        private int timeoutMs;

        private Builder(String baseUrl, String model) {
            this.baseUrl = baseUrl;
            this.model = model;
        }

        public Builder apiKey(Supplier<String> supplier) {
            this.apiKeySupplier = supplier;
            return this;
        }

        public Builder socksProxy(String host, int port) {
            this.proxyHost = host;
            this.proxyPort = port;
            return this;
        }

        public Builder timeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public LlmConfig build() {
            return new LlmConfig(this);
        }
    }
}
