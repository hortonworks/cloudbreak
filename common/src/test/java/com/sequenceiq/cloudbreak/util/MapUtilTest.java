package com.sequenceiq.cloudbreak.util;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MapUtilTest {

    private static final String TEST_KEY = "something";

    private static final String TEST_VALUE = "someOtherThing";

    @Test
    void testWhenInputIsEmptyThenEmptyShouldReturn() {
        Map<String, Object> result = MapUtil.cleanMap(emptyMap());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testWhenInputIsNullThenEmptyShouldReturn() {
        Map<String, Object> result = MapUtil.cleanMap(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testWhenAllTheValuesAreNullThenEmptyShouldReturn() {
        Map<String, Object> input = new LinkedHashMap<>(1);
        input.put(TEST_KEY, null);

        Map<String, Object> result = MapUtil.cleanMap(input);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testWhenAllTheValuesAreNullAsStringThenEmptyShouldReturn() {
        Map<String, Object> result = MapUtil.cleanMap(Map.of(TEST_KEY, "null"));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testWhenAllTheValuesAreEmptyStringThenEmptyShouldReturn() {
        Map<String, Object> result = MapUtil.cleanMap(Map.of(TEST_KEY, ""));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testWhenTheValueIsNotNullThenNonEmptyShouldReturn() {
        Map<String, Object> result = MapUtil.cleanMap(Map.of(TEST_KEY, TEST_VALUE));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_KEY, new ArrayList<>(result.keySet()).get(0));
        assertEquals(TEST_VALUE, result.get(TEST_KEY));
    }

}