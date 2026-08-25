package io.donbee.llm;

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

    /** Model identifier this brain is currently configured with (for logging). */
    String model();
}
