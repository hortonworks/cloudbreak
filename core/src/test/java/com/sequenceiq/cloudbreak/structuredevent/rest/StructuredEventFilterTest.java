package com.sequenceiq.cloudbreak.structuredevent.rest;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class StructuredEventFilterTest {

    private StructuredEventFilter underTest = new StructuredEventFilter();

    @Test
    public void testResourceIdParsingWhenValidJsonIsReturned() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params, "{\"id\": \"12345\"}");
        assertEquals(params.get(RESOURCE_ID), "12345", "Should find resourceId in valid JSON response");
    }

    @Test
    public void testResourceIdParsingWhenNonJson() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params, "\"id\":12345 Something other written here");
        assertEquals(params.get(RESOURCE_ID), "12345", "Should find resourceId in response");
    }

    @Test
    public void testResourceIdParsingWhenJsonButNoId() {
        Map<String, String> params = new HashMap<>();
        underTest.extendRestParamsFromResponse(params, "{\"message\": \"Error happened and responding with JSON\"}");
        assertTrue(params.isEmpty(), "No ResourceId is present in responseBody");
    }

    @Test
    public void testResourceIdParsingWhenPlainTextResponse() {
        Map<String, String> params = Collections.emptyMap();
        underTest.extendRestParamsFromResponse(params,
                "The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character");
        assertTrue(params.isEmpty(), "No ResourceId is present in responseBody");
    }
}