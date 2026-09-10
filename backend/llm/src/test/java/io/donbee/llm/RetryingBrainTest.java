package io.donbee.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class RetryingBrainTest {

    /** Fake brain failing N times before succeeding. */
    private static class FlakyBrain implements Brain {
        final AtomicInteger calls = new AtomicInteger();
        final int failTimes;
        final String failMsg;

        FlakyBrain(int failTimes, String failMsg) {
            this.failTimes = failTimes;
            this.failMsg = failMsg;
        }

        @Override
        public String respond(String systemPrompt, String userPrompt) {
            if (calls.incrementAndGet() <= failTimes) {
                throw new BrainException(failMsg);
            }
            return "ok";
        }

        @Override
        public String model() {
            return "flaky";
        }
    }

    @Test
    public void Given_FailsTwice_When_RetriedWithZeroGaps_Then_SucceedsOnThirdAttempt() {
        // 2 gaps = 3 total attempts
        RetryingBrain brain = new RetryingBrain(
            new FlakyBrain(2, "transient"), List.of(Duration.ZERO, Duration.ZERO));

        String out = brain.respond("sys", "user");

        assertThat(out).isEqualTo("ok");
    }

    @Test
    public void Given_AllAttemptsFail_When_Retried_Then_WrappedExceptionWithCount() {
        RetryingBrain brain = new RetryingBrain(
            new FlakyBrain(99, "provider down"), List.of(Duration.ZERO, Duration.ZERO));

        assertThatThrownBy(() -> brain.respond("sys", "user"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("failed after 3 attempts")
            .hasMessageContaining("provider down");
    }

    @Test
    public void Given_FlakyBrain_When_Retried_Then_DelegateCalledExactlyAttemptCount() {
        FlakyBrain flaky = new FlakyBrain(99, "x");
        RetryingBrain brain = new RetryingBrain(flaky, List.of(Duration.ZERO, Duration.ZERO));

        try {
            brain.respond(null, "u");
        } catch (BrainException expected) {
        }

        assertThat(flaky.calls.get()).isEqualTo(3); // 1 initial + 2 retries
    }

    @Test
    public void Given_SuccessOnFirstCall_When_Retried_Then_NoExtraCalls() {
        FlakyBrain flaky = new FlakyBrain(0, "never");
        RetryingBrain brain = new RetryingBrain(flaky, List.of(Duration.ZERO));

        assertThat(brain.respond(null, "u")).isEqualTo("ok");
        assertThat(flaky.calls.get()).isEqualTo(1);
    }

    @Test
    public void Given_DefaultGaps_When_Constructed_Then_ThreeRetriesWithGrowingGaps() {
        assertThat(RetryingBrain.DEFAULT_GAPS)
            .containsExactly(Duration.ofSeconds(5), Duration.ofSeconds(15), Duration.ofSeconds(45));
    }

    @Test
    public void Given_ToolsVariant_When_Retried_Then_RetriesApplyToo() {
        RetryingBrain brain = new RetryingBrain(
            new FlakyBrain(1, "cli hiccup"), List.of(Duration.ZERO));

        assertThat(brain.respond("s", "u", List.of())).isEqualTo("ok");
    }

    @Test
    public void Given_Delegate_When_ModelQueried_Then_DelegatesModel() {
        assertThat(new RetryingBrain(new FlakyBrain(0, "x")).model()).isEqualTo("flaky");
    }

    @Test
    public void Given_InterruptDuringGap_When_Retried_Then_InterruptedBrainException() {
        RetryingBrain brain = new RetryingBrain(
            new FlakyBrain(99, "x"), List.of(Duration.ofSeconds(60)));

        Thread testThread = Thread.currentThread();
        Thread killer = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            testThread.interrupt();
        });
        killer.start();

        assertThatThrownBy(() -> brain.respond(null, "u"))
            .isInstanceOf(BrainException.class)
            .hasMessageContaining("interrupted");
    }
}
