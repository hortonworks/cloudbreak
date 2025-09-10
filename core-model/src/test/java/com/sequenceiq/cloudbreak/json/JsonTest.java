package com.sequenceiq.cloudbreak.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.Json;

class JsonTest {

    @Test
    void testGetValueWithTwoDepth() throws JsonProcessingException {
        Map<String, Map<String, String>> map = Map.of("depth1", Map.of("depth2", "value"));

        Json json = new Json(map);

        String value = json.getString("depth1.depth2");

        assertEquals("value", value);
    }

    @Test
    void testGetValueWithTwoDepthAndInt() throws JsonProcessingException {
        Map<String, Map<String, Integer>> map = Map.of("depth1", Map.of("depth2", 1));

        Json json = new Json(map);

        Integer value = json.getInt("depth1.depth2");

        assertEquals(1, value);
    }

    @Test
    void testGetValueWithThreeDepthAndInt() throws JsonProcessingException {
        Map<String, Map<String, Map<String, String>>> map = Map.of("depth1", Map.of("depth2", Map.of("depth3", "value")));

        Json json = new Json(map);

        String value = json.getString("depth1.depth2.depth3");

        assertEquals("value", value);
    }

    @Test
    void testGetValueWithTwoDepthAndObject() throws JsonProcessingException {
        Map<String, Map<String, Map<String, String>>> map = Map.of("depth1", Map.of("depth2", Map.of("depth3", "value")));

        Json json = new Json(map);

        JsonNode value = json.getJsonNode("depth1.depth2");

        assertEquals("{\"depth3\":\"value\"}", value.toString());
    }

    @Test
    void test() {
        String js = "{\"depth1\":{\"depth2\":\"value\"}}";

        String value = new Json(js).getString("depth1.depth2");

        assertEquals("value", value);
    }
}
