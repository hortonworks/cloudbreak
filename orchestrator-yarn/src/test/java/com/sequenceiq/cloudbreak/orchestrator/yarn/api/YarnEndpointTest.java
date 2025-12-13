package com.sequenceiq.cloudbreak.orchestrator.yarn.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class YarnEndpointTest {

    @Test
    void testRemoveLeadingAndTrailingSlash() {
        String testString = "//test/";
        String expectedString = "test";
        YarnEndpoint yarnEndpoint = new YarnEndpoint("", "");
        assertEquals(expectedString, yarnEndpoint.removeLeadingAndTrailingSlash(testString));
    }
}