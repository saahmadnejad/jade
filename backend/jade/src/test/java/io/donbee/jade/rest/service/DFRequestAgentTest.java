package io.donbee.jade.rest.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

public class DFRequestAgentTest {

    @Test
    public void Given_RapidSuccessiveRequests_When_TempAgentNamesGenerated_Then_AllNamesUnique() {
        // --- Arrange ---
        Set<String> names = ConcurrentHashMap.newKeySet();

        // --- Act ---
        for (int i = 0; i < 1000; i++) {
            names.add(DFRequestAgent.nextAgentName());
        }

        // --- Assert ---
        // Same-millisecond requests previously collided ("df_req_<millis>"),
        // producing NameClashException and HTTP 500s on the DF endpoints.
        assertThat(names).hasSize(1000);
    }
}
