package com.sequenceiq.cloudbreak.common.mappable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MappableTest {

    private static final String TEST_MAP_KEY = "key";

    private TestMappable underTest;

    @BeforeEach
    void setUp() {
        underTest = new TestMappable();
    }

    @Test
    @DisplayName("Test getObject when the input parameter Map is null")
    void testGetObjectNullParams() {
        assertNull(underTest.getObject(null, TEST_MAP_KEY, String.class));
    }

    @Test
    @DisplayName("Test getObject when the input parameter Map does not contain the given key")
    void testGetObjectNoKey() {
        assertNull(underTest.getObject(Map.of(), TEST_MAP_KEY, String.class));
    }

    @Test
    @DisplayName("Test getObject when the input parameter Map contains the given key but the value is null")
    void testGetObjectNullValue() {
        Map<String, Object> params = new HashMap<>();
        params.put(TEST_MAP_KEY, null);

        assertNull(underTest.getObject(params, TEST_MAP_KEY, String.class));
    }

    @Test
    @DisplayName("Test getObject when the input parameter Map contains the given key but the value is not of the given class")
    void testGetObjectInvalidType() {
        Map<String, Object> params = new HashMap<>();
        params.put(TEST_MAP_KEY, 1);

        assertNull(underTest.getObject(params, TEST_MAP_KEY, String.class));
    }

    @Test
    @DisplayName("Test getObject when the input parameter Map contains the given key")
    void testGetObject() {
        String value = "value";

        assertEquals(value, underTest.getObject(Map.of(TEST_MAP_KEY, value), TEST_MAP_KEY, String.class));
    }

    private static class TestMappable implements Mappable {
        @Override
        public Map<String, Object> asMap() {
            return Map.of();
        }

        @Override
        public CloudPlatform getCloudPlatform() {
            return null;
        }
    }

}