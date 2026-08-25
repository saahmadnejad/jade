package io.donbee.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * {@link Brain} backed by any OpenAI-compatible chat-completions endpoint
 * (OpenRouter, OpenAI, Ollama, vLLM, ...) with optional SOCKS5 proxying.
 */
public class HttpBrain implements Brain {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmConfig config;
    private final HttpClient client;

    public HttpBrain(LlmConfig config) {
        this.config = config;
        HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.timeoutMs()));
        if (config.useProxy()) {
            builder.proxy(ProxySelector.of(
                new InetSocketAddress(config.proxyHost(), config.proxyPort())));
        }
        this.client = builder.build();
    }

    @Override
    public String respond(String systemPrompt, String userPrompt) {
        String apiKey = config.apiKeySupplier().get();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BrainException("No API key configured for model " + config.model()
                + ". Set the environment variable or the local secrets file (ADR-0002).");
        }

        String payload = buildPayload(systemPrompt, userPrompt).toString();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofMillis(config.timeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        } catch (Exception e) {
            throw new BrainException("Invalid LLM endpoint: " + config.baseUrl(), e);
        }

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new BrainException("LLM request failed for model " + config.model() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrainException("LLM request interrupted", e);
        }

        return extractContent(response);
    }

    /** Visible for tests. */
    ObjectNode buildPayload(String systemPrompt, String userPrompt) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", config.model());
        ArrayNode messages = root.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            addMessage(messages, "system", systemPrompt);
        }
        addMessage(messages, "user", userPrompt);
        return root;
    }

    private static void addMessage(ArrayNode messages, String role, String content) {
        ObjectNode msg = messages.addObject();
        msg.put("role", role);
        msg.put("content", content);
    }

    private String extractContent(HttpResponse<String> response) {
        String body = response.body();
        if (response.statusCode() / 100 != 2) {
            throw new BrainException("LLM provider returned HTTP " + response.statusCode()
                + " for model " + config.model() + ": " + truncate(body));
        }
        try {
            JsonNode json = MAPPER.readTree(body);
            JsonNode content = json.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new BrainException("LLM response missing choices[0].message.content: " + truncate(body));
            }
            return content.asText();
        } catch (BrainException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new BrainException("Failed to parse LLM response: " + truncate(body), e);
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 500 ? s : s.substring(0, 500) + "...";
    }

    @Override
    public String model() {
        return config.model();
    }
}
