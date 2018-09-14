package com.sequenceiq.cloudbreak.json;

import static org.hamcrest.core.Is.is;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.domain.json.Json;

import net.sf.json.JSONObject;

public class JsonTest {

    @Test
    public void testGetValueWithTwoDepth() throws JsonProcessingException {
        Map<String, Map<String, String>> map = Map.of("depth1", Map.of("depth2", "value"));

        Json json = new Json(map);

        String value = json.getValue("depth1.depth2");

        Assert.assertThat(value, is("value"));
    }

    @Test
    public void testGetValueWithTwoDepthAndInt() throws JsonProcessingException {
        Map<String, Map<String, Integer>> map = Map.of("depth1", Map.of("depth2", 1));

        Json json = new Json(map);

        Integer value = json.getValue("depth1.depth2");

        Assert.assertThat(value, is(1));
    }

    @Test
    public void testGetValueWithThreeDepthAndInt() throws JsonProcessingException {
        Map<String, Map<String, Map<String, String>>> map = Map.of("depth1", Map.of("depth2", Map.of("depth3", "value")));

        Json json = new Json(map);

        String value = json.getValue("depth1.depth2.depth3");

        Assert.assertThat(value, is("value"));
    }

    @Test
    public void testGetValueWithTwoDepthAndObject() throws JsonProcessingException {
        Map<String, Map<String, Map<String, String>>> map = Map.of("depth1", Map.of("depth2", Map.of("depth3", "value")));

        Json json = new Json(map);

        JSONObject value = json.getValue("depth1.depth2");

        Assert.assertThat(value.toString(), is("{\"depth3\":\"value\"}"));
    }

    @Test
    public void test() {
        String js = "{\"depth1\":{\"depth2\":\"value\"}}";

        String value = new Json(js).getValue("depth1.depth2");

        Assert.assertThat(value, is("value"));
    }
}
