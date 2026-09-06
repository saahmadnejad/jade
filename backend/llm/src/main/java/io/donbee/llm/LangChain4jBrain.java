package io.donbee.llm;

import dev.langchain4j.agent.tool.JsonSchemaProperty;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * {@link Brain} backed by any OpenAI-compatible chat endpoint (9router), implemented
 * via langchain4j's {@link OpenAiChatModel}.
 *
 * <p>Strips the {@code 9router/} prefix from model names for backward
 * compatibility. Supports tool calling: when {@link #respond(String, String, List)}
 * receives non-empty tools, it runs an agent loop (request, generate tool calls,
 * execute locally, feed results back, continue until text).
 *
 * <p>No JADE/platform dependencies (ADR-0001): this class lives in the
 * standalone {@code llm} module.
 */
public class LangChain4jBrain implements Brain {

    private static final String NINEROUTER_PREFIX = "9router/";

    private final ChatLanguageModel model;
    private final String modelName;

    private LangChain4jBrain(ChatLanguageModel model, String modelName) {
        this.model = model;
        this.modelName = modelName;
    }

    /** Strip the legacy {@code 9router/} prefix if present. */
    static String normalizeModelName(String model) {
        if (model != null && model.startsWith(NINEROUTER_PREFIX)) {
            return model.substring(NINEROUTER_PREFIX.length());
        }
        return model;
    }

    @Override
    public String respond(String systemPrompt, String userPrompt) {
        try {
            return withRateLimitRetry(() -> {
                List<ChatMessage> messages = new ArrayList<>();
                if (systemPrompt != null && !systemPrompt.isBlank()) {
                    messages.add(SystemMessage.from(systemPrompt));
                }
                messages.add(UserMessage.from(userPrompt));
                Response<AiMessage> response = model.generate(messages);
                AiMessage ai = response.content();
                String text = ai != null ? ai.text() : null;
                if (text == null || text.isBlank()) {
                    throw new BrainException("LLM returned no content for model " + modelName);
                }
                return text;
            });
        } catch (Exception e) {
            if (e instanceof BrainException) {
                throw (BrainException) e;
            }
            throw new BrainException("LLM request failed for model " + modelName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Agent loop: send prompt + tools to LLM, execute tool calls locally,
     * feed results back, continue until LLM returns text.
     */
    @Override
    public String respond(String systemPrompt, String userPrompt, List<Tool> tools) {
        try {
            return withRateLimitRetry(() -> respondWithTools(systemPrompt, userPrompt, tools));
        } catch (Exception e) {
            if (e instanceof BrainException) {
                throw (BrainException) e;
            }
            throw new BrainException("LLM request failed for model " + modelName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Free-tier providers (9router upstream pools) answer 429 with a
     * "reset after Ns" window. Retry up to {@code MAX_RETRIES} times, waiting
     * the announced window (plus slack) between attempts.
     */
    private static final int MAX_RETRIES = 3;

    private <T> T withRateLimitRetry(java.util.function.Supplier<T> call) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                long waitMs = announcedResetMs(e.getMessage());
                if (waitMs <= 0 || attempt == MAX_RETRIES) {
                    throw e;
                }
                System.err.println("[LangChain4jBrain] rate limited, waiting "
                    + waitMs + "ms (attempt " + (attempt + 1) + "/" + MAX_RETRIES + "): "
                    + e.getMessage());
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                last = e;
            }
        }
        throw last;
    }

    /** Extracts "reset after 2m 45s"-style windows from provider error bodies; -1 when absent. */
    static long announcedResetMs(String message) {
        if (message == null) {
            return -1;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("reset after (?:(\\d+)m)?\\s*(?:(\\d+)s)?").matcher(message);
        long total = -1;
        while (m.find()) {
            long minutes = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
            long seconds = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
            if (minutes == 0 && seconds == 0) {
                continue;
            }
            total = Math.max(total, (minutes * 60 + seconds) * 1000L);
        }
        return total < 0 ? -1 : total + 5000;
    }

    private String respondWithTools(String systemPrompt, String userPrompt, List<Tool> tools) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(UserMessage.from(userPrompt));

        List<ToolSpecification> specs = tools != null ? toSpecifications(tools) : List.of();
        int maxIterations = 20;

        for (int i = 0; i < maxIterations; i++) {
            Response<AiMessage> response = model.generate(messages, specs);
            AiMessage ai = response.content();

            List<ToolExecutionRequest> toolRequests = ai != null ? ai.toolExecutionRequests() : null;
            if (toolRequests == null || toolRequests.isEmpty()) {
                String text = ai != null ? ai.text() : null;
                if (text == null || text.isBlank()) {
                    throw new BrainException("LLM returned no content for model " + modelName);
                }
                return text;
            }

            for (ToolExecutionRequest req : toolRequests) {
                String result = executeTool(tools, req.name(), req.arguments());
                messages.add(ai);
                messages.add(ToolExecutionResultMessage.from(req, result));
            }
        }
        throw new BrainException("Agent loop exceeded max iterations for model " + modelName);
    }

    private String executeTool(List<Tool> tools, String name, String args) {
        for (Tool t : tools) {
            if (t.name().equals(name)) {
                System.out.println("[LangChain4jBrain] tool: " + name + "(" + args + ")");
                return t.call(args);
            }
        }
        return "Tool not found: " + name;
    }

    private static List<ToolSpecification> toSpecifications(List<Tool> tools) {
        List<ToolSpecification> specs = new ArrayList<>();
        for (Tool t : tools) {
            ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(t.name())
                .description(t.description());
            Map<String, String> params = t.parameters();
            Map<String, Boolean> req = t.required();
            for (Map.Entry<String, String> p : params.entrySet()) {
                JsonSchemaProperty[] props = {
                    JsonSchemaProperty.type(p.getValue()),
                    JsonSchemaProperty.description(p.getKey())
                };
                if (Boolean.TRUE.equals(req.get(p.getKey()))) {
                    builder.addParameter(p.getKey(), props);
                } else {
                    builder.addOptionalParameter(p.getKey(), props);
                }
            }
            specs.add(builder.build());
        }
        return specs;
    }

    @Override
    public String model() {
        return modelName;
    }

    /**
     * Build an OpenAI-compatible brain via langchain4j.
     */
    public static LangChain4jBrain build(String baseUrl, String model,
                                         Supplier<String> apiKeySupplier, Duration timeout) {
        return build(baseUrl, model, apiKeySupplier, timeout, null, 0, null);
    }

    /** Build with optional SOCKS5 proxy (host/port ignored when host is null). */
    public static LangChain4jBrain build(String baseUrl, String model,
                                         Supplier<String> apiKeySupplier, Duration timeout,
                                         String proxyHost, int proxyPort, Path workDir) {
        String rawModel = normalizeModelName(model);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(rawModel)
            .timeout(timeout);

        String key = apiKeySupplier.get();
        if (key != null && !key.isBlank()) {
            builder.apiKey(key);
        } else {
            builder.apiKey("dummy");
        }

        if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            builder.proxy(new Proxy(Proxy.Type.SOCKS,
                new InetSocketAddress(proxyHost, proxyPort)));
        }

        return new LangChain4jBrain(builder.build(), rawModel);
    }
}
