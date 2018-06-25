package com.sequenceiq.cloudbreak.orchestrator.yarn.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class YarnEndpointTest {

    @Test
    public void testRemoveLeadingAndTrailingSlash() {
        String testString = "//test/";
        String expectedString = "test";
        YarnEndpoint yarnEndpoint = new YarnEndpoint("", "");
        assertEquals(expectedString, yarnEndpoint.removeLeadingAndTrailingSlash(testString));
    }
}