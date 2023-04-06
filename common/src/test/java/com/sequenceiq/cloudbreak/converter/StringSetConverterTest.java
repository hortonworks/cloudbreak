package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class StringSetConverterTest {

    private final StringSetConverter underTest = new StringSetConverter();

    @Test
    public void shouldConvertStringToSetByComma() {
        String input = "1,2,3";
        Set<String> expectedResult = Set.of("1", "2", "3");

        Set<String> result = underTest.convertToEntityAttribute(input);

        assertEquals(3, result.size());
        assertTrue(result.containsAll(expectedResult));
    }

    @Test
    public void shouldConvertNullOrEmptyToNull() {
        assertNull(underTest.convertToDatabaseColumn(null));
        assertNull(underTest.convertToDatabaseColumn(Collections.emptySet()));
        assertNull(underTest.convertToEntityAttribute(null));
    }
}
