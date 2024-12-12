package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ReservedTagValidatorServiceTest {

    private final ReservedTagValidatorService underTest = new ReservedTagValidatorService();

    @Test
    public void testIsCodClusterTagThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> underTest.validateInternalTags(Map.of("is_cod_cluster", "true")));

        assertEquals("is_cod_cluster is a reserved tag. Please don't use it.", exception.getMessage());
    }

    @Test
    public void testNullTagsDoesNotThrowException() {
        assertDoesNotThrow(() -> underTest.validateInternalTags(null));
    }

    @Test
    public void testEmptyTagsMapDoesNotThrowException() {
        assertDoesNotThrow(() -> underTest.validateInternalTags(Map.of()));
    }

    @Test
    public void testDoesNotThrowWhenReservedWordIsNotUsed() {
        assertDoesNotThrow(() -> underTest.validateInternalTags(Map.of("a", "a1")));
    }
}