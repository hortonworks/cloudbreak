package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.common.model.ProviderSyncState;

class ProviderSyncSetToStringConverterTest {

    private ProviderSyncSetToStringConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ProviderSyncSetToStringConverter();
    }

    @Test
    void testConvertToDatabaseColumnWithNull() {
        String result = converter.convertToDatabaseColumn(null);
        assertNull(result);
    }

    @Test
    void testConvertToDatabaseColumnWithEmptySet() {
        String result = converter.convertToDatabaseColumn(new HashSet<>());
        assertNull(result);
    }

    @Test
    void testConvertToDatabaseColumnWithNonEmptySet() {
        Set<ProviderSyncState> states = Set.of(ProviderSyncState.VALID, ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED);
        String result = converter.convertToDatabaseColumn(states);
        assertTrue(result.contains("VALID"));
        assertTrue(result.contains("BASIC_SKU_MIGRATION_NEEDED"));
        assertEquals(2, result.split(",").length);
    }

    @Test
    void testConvertToEntityAttributeWithNull() {
        Set<ProviderSyncState> result = converter.convertToEntityAttribute(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertToEntityAttributeWithEmptyString() {
        Set<ProviderSyncState> result = converter.convertToEntityAttribute("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertToEntityAttributeWithNonEmptyString() {
        String dbData = "VALID,BASIC_SKU_MIGRATION_NEEDED";
        Set<ProviderSyncState> result = converter.convertToEntityAttribute(dbData);
        assertEquals(Set.of(ProviderSyncState.VALID, ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED), result);
    }
}