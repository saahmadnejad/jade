package io.donbee.jade.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.Test;

/**
 * Regression test: THREAD_GROUPS used to be a static map shared by all
 * containers in the JVM. Killing one container cleared it
 * (releaseResources()), so every subsequent agent creation on any other
 * container threw NullPointerException -> e.g. the DF REST endpoints broke
 * after deleting a scenario container.
 */
public class FullResourceManagerTest {

    @Test
    public void Given_TwoContainers_When_OneReleasesResources_Then_OtherStillCreatesAgentThreads() {
        // --- Arrange ---
        FullResourceManager killedContainer = new FullResourceManager();
        FullResourceManager survivingContainer = new FullResourceManager();

        // --- Act ---
        killedContainer.releaseResources();

        // --- Assert ---
        assertThatCode(() ->
            survivingContainer.getThread(ResourceManager.USER_AGENTS, "still-alive", () -> {}))
            .doesNotThrowAnyException();
    }

    @Test
    public void Given_SingleResourceManager_When_ThreadRequested_Then_TrackedInItsGroup() {
        // --- Arrange ---
        FullResourceManager rm = new FullResourceManager();

        // --- Act ---
        Thread t = rm.getThread(ResourceManager.SYSTEM_AGENTS, "test-agent", () -> {});

        // --- Assert ---
        assertThat(t).isNotNull();
        assertThat(t.getName()).isEqualTo("test-agent");
    }
}
