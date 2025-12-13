package com.sequenceiq.redbeams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UuidGeneratorServiceTest {

    private static final String UUID_STRING = "abcdefgh-ijkl-1234-5678-mnopqrstuxyv";

    private static final String NOT_MATCHING_UUID_STRING = "abc";

    private UuidGeneratorService underTest;

    @BeforeEach
    public void setup() {
        underTest = new UuidGeneratorService();
    }

    @Test
    void testRandomUuid() throws IllegalArgumentException {
        String randomUuid = underTest.randomUuid();
        UUID.fromString(randomUuid);
    }

    @Test
    void testGenerateUuidVariablePart() {
        String password = underTest.uuidVariableParts(30, UUID_STRING);

        assertEquals("abcdefghijkl234678mnopqrstuxyv", password);
    }

    @Test
    void testGenerateUuidVariablePartWhenLengthShorter() {
        String password = underTest.uuidVariableParts(15, UUID_STRING);

        assertEquals("abcdefghijkl234", password);
    }

    @Test
    void testGenerateUuidVariablePartWhenLengthBelow1() {
        String password = underTest.uuidVariableParts(0, UUID_STRING);

        assertEquals("", password);
    }

    @Test
    void testGenerateUuidVariableWhenUuidPatternNotMatching() {
        String password = underTest.uuidVariableParts(30, NOT_MATCHING_UUID_STRING);

        assertEquals("abc", password);
    }
}
