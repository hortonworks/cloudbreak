package com.sequenceiq.cloudbreak.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringSetToStringConverterTest {
    private StringSetToStringConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StringSetToStringConverter();
    }

    @Test
    void testConvertToDatabaseColumn() {
        String dbColumn = underTest.convertToDatabaseColumn(new LinkedHashSet<>(Arrays.asList("col1", "col2")));
        assertEquals("col1,col2", dbColumn);
    }

    @Test
    void testConvertToDatabaseColumnWhenEmpty() {
        String dbColumn = underTest.convertToDatabaseColumn(Set.of());
        assertNull(dbColumn);
    }

    @Test
    void testConvertToDatabaseColumnWhenNull() {
        String dbColumn = underTest.convertToDatabaseColumn(null);
        assertNull(dbColumn);
    }

    @Test
    void testConvertToEntityAttribute() {
        Set<String> attributes = underTest.convertToEntityAttribute("col1,col2");
        assertEquals(Set.of("col1", "col2"), attributes);
    }

    @Test
    void testConvertToEntityAttributeWhenEmptyString() {
        Set<String> attributes = underTest.convertToEntityAttribute("");
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testConvertToEntityAttributeWhenNull() {
        Set<String> attributes = underTest.convertToEntityAttribute(null);
        assertTrue(attributes.isEmpty());
    }
}
