package com.sequenceiq.redbeams.service;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class UuidGeneratorServiceTest {

    private static final String UUID_STRING = "abcdefgh-ijkl-1234-5678-mnopqrstuxyv";

    private static final String NOT_MATCHING_UUID_STRING = "abc";

    private UuidGeneratorService underTest;

    @Before
    public void setup() {
        underTest = new UuidGeneratorService();
    }

    @Test
    public void testRandomUuid() throws IllegalArgumentException {
        String randomUuid = underTest.randomUuid();
        UUID.fromString(randomUuid);
    }

    @Test
    public void testGenerateUuidVariablePart() {
        String password = underTest.uuidVariableParts(30, UUID_STRING);

        assertEquals("abcdefghijkl234678mnopqrstuxyv", password);
    }

    @Test
    public void testGenerateUuidVariablePartWhenLengthShorter() {
        String password = underTest.uuidVariableParts(15, UUID_STRING);

        assertEquals("abcdefghijkl234", password);
    }

    @Test
    public void testGenerateUuidVariablePartWhenLengthBelow1() {
        String password = underTest.uuidVariableParts(0, UUID_STRING);

        assertEquals("", password);
    }

    @Test
    public void testGenerateUuidVariableWhenUuidPatternNotMatching() {
        String password = underTest.uuidVariableParts(30, NOT_MATCHING_UUID_STRING);

        assertEquals("abc", password);
    }
}
