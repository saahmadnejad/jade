package io.donbee.jade.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FIPANamesTest {

    @Test
    public void Given_FIPANamesInterface_When_AmsConstantRead_Then_ValueIsAms() {
        // --- Arrange ---
        String expected = "ams";

        // --- Act ---
        String actual = FIPANames.AMS;

        // --- Assert ---
        assertEquals(expected, actual);
    }

    @Test
    public void Given_FIPANamesInteractionProtocol_When_FipaRequestRead_Then_ValueMatchesFipaSpec() {
        // --- Arrange ---
        String expected = "fipa-request";

        // --- Act ---
        String actual = FIPANames.InteractionProtocol.FIPA_REQUEST;

        // --- Assert ---
        assertEquals(expected, actual);
    }
}
