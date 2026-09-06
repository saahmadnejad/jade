package io.donbee.llm;

import java.util.List;
import java.util.Map;

/**
 * The reasoning capability of an agent: a prompt goes in, generated text
 * comes out. Implementations decide where the intelligence lives (HTTP LLM
 * provider, local CLI tool, test stub, ...).
 *
 * <p>This library is framework-agnostic (see ADR-0001): it must not depend on
 * any agent platform.</p>
 */
public interface Brain {

    /**
     * Generate a response for {@code userPrompt} under {@code systemPrompt}.
     *
     * @param systemPrompt role/persona instructions for this brain
     * @param userPrompt   the task-specific input
     * @return the generated text
     * @throws BrainException on transport, auth or provider errors
     */
    String respond(String systemPrompt, String userPrompt);

    /**
     * Generate a response, optionally invoking tools. When {@code tools} is
     * non-empty the brain must implement an agent loop: the LLM is told the
     * tool names/descriptions/parameters, may generate tool calls, the brain
     * executes each locally, feeds results back, and continues until the LLM
     * returns text.
     *
     * @param systemPrompt role/persona instructions
     * @param userPrompt   the task-specific input
     * @param tools        tools the LLM may invoke (may be empty)
     * @return the generated text
     * @throws BrainException on transport, auth, or provider errors
     */
    default String respond(String systemPrompt, String userPrompt, List<Tool> tools) {
        return respond(systemPrompt, userPrompt);
    }

    /** Model identifier this brain is currently configured with. */
    String model();

    /**
     * A tool the LLM may invoke during reasoning. The brain implementation
     * decides how to execute it (shell out to bash, opencode CLI, etc.).
     */
    interface Tool {
        /** Function name the LLM calls. */
        String name();

        /** Human-readable description. */
        String description();

        /** Parameter name to JSON-schema type (e.g. {@code "string"}, {@code "integer"}). */
        Map<String, String> parameters();

        /** Parameter name to whether it is required (defaults to all required). */
        default Map<String, Boolean> required() {
            return parameters().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, e -> true));
        }

        /** Execute the tool and return the result text. */
        String call(String argumentsJson);
    }
}
