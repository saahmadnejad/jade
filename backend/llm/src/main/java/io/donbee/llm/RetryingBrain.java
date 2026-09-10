package io.donbee.llm;

import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Decorator that retries a {@link Brain} call with growing gaps between
 * attempts. Any {@link BrainException} (timeout, CLI failure, provider
 * error) is retryable; the last failure is rethrown once attempts are
 * exhausted. Pair with {@link FallbackBrain} so a different model/CLI takes
 * over when retries are exhausted.
 */
public class RetryingBrain implements Brain {

    private static final Logger LOG = Logger.getLogger(RetryingBrain.class.getName());

    /** Default gaps between attempts: 5s, 15s, 45s. */
    public static final List<Duration> DEFAULT_GAPS = List.of(
        Duration.ofSeconds(5), Duration.ofSeconds(15), Duration.ofSeconds(45));

    private final Brain delegate;
    private final List<Duration> gaps;

    /** Production constructor: 3 retries with the default 5/15/45s gaps. */
    public RetryingBrain(Brain delegate) {
        this(delegate, DEFAULT_GAPS);
    }

    /** Test constructor: inject custom (zero/short) gaps. */
    RetryingBrain(Brain delegate, List<Duration> gaps) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate brain required");
        }
        this.delegate = delegate;
        this.gaps = List.copyOf(gaps == null ? DEFAULT_GAPS : gaps);
    }

    @Override
    public String respond(String systemPrompt, String userPrompt) {
        return respondWithRetry(() -> delegate.respond(systemPrompt, userPrompt));
    }

    @Override
    public String respond(String systemPrompt, String userPrompt, List<Tool> tools) {
        return respondWithRetry(() -> delegate.respond(systemPrompt, userPrompt, tools));
    }

    private String respondWithRetry(java.util.function.Supplier<String> call) {
        BrainException last = null;
        for (int attempt = 0; attempt <= gaps.size(); attempt++) {
            if (attempt > 0) {
                sleep(gaps.get(attempt - 1));
            }
            try {
                return call.get();
            } catch (BrainException e) {
                last = e;
                LOG.warning("brain " + delegate.model() + " attempt " + (attempt + 1)
                    + "/" + (gaps.size() + 1) + " failed: " + e.getMessage());
            }
        }
        throw new BrainException("brain " + delegate.model() + " failed after "
            + (gaps.size() + 1) + " attempts; last error: "
            + (last != null ? last.getMessage() : "unknown"), last);
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrainException("retry interrupted", e);
        }
    }

    @Override
    public String model() {
        return delegate.model();
    }
}
