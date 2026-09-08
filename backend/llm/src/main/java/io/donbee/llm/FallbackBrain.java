package io.donbee.llm;

import java.util.List;

/**
 * Tries a list of brains in order; the first non-failing response wins.
 * Used to fall back from a flaky/timing-out primary (e.g. an agentic CLI)
 * to a reliable model.
 */
public class FallbackBrain implements Brain {

    private static final java.util.logging.Logger LOG =
        java.util.logging.Logger.getLogger(FallbackBrain.class.getName());

    private final List<Brain> delegates;

    public FallbackBrain(List<Brain> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("at least one brain required");
        }
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public String respond(String systemPrompt, String userPrompt, List<Tool> tools) {
        BrainException last = null;
        for (Brain brain : delegates) {
            try {
                return brain.respond(systemPrompt, userPrompt, tools);
            } catch (BrainException e) {
                last = e;
                LOG.warning("brain " + brain.model()
                    + " failed, trying next: " + e.getMessage());
            }
        }
        throw new BrainException("All "
            + delegates.size() + " brains failed; last error: "
            + (last != null ? last.getMessage() : "unknown"), last);
    }

    @Override
    public String respond(String systemPrompt, String userPrompt) {
        BrainException last = null;
        for (Brain brain : delegates) {
            try {
                return brain.respond(systemPrompt, userPrompt);
            } catch (BrainException e) {
                last = e;
                LOG.warning("brain " + brain.model()
                    + " failed, trying next: " + e.getMessage());
            }
        }
        throw new BrainException("All "
            + delegates.size() + " brains failed; last error: "
            + (last != null ? last.getMessage() : "unknown"), last);
    }

    @Override
    public String model() {
        return delegates.get(0).model();
    }
}
