package com.sequenceiq.cloudbreak.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtilTest {

    private ObjectMapper getMapper() throws Exception {
        Field field = JsonUtil.class.getDeclaredField("MAPPER");
        field.setAccessible(true);
        return (ObjectMapper) field.get(null);
    }

    private ObjectMapper getStrictMapper() throws Exception {
        Field field = JsonUtil.class.getDeclaredField("STRICT_MAPPER");
        field.setAccessible(true);
        return (ObjectMapper) field.get(null);
    }

    @Test
    public void testMapperConfiguration() throws Exception {
        ObjectMapper mapper = getMapper();

        // Verify disabled features
        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertFalse(mapper.isEnabled(SerializationFeature.FAIL_ON_SELF_REFERENCES));

        // Verify enabled features
        assertTrue(mapper.isEnabled(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS));
        assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY));
        assertTrue(mapper.isEnabled(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL));

        // Verify configured features
        assertFalse(mapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertFalse(mapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
    }

    @Test
    public void testStrictMapperConfiguration() throws Exception {
        ObjectMapper strictMapper = getStrictMapper();

        // Verify that STRICT_MAPPER inherits MAPPER's configuration
        ObjectMapper mapper = getMapper();
        assertEquals(mapper.getDeserializationConfig().getDeserializationFeatures() & ~DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.getMask(),
                strictMapper.getDeserializationConfig().getDeserializationFeatures() & ~DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.getMask());

        // Verify STRICT_MAPPER specific configuration
        assertTrue(strictMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }
}