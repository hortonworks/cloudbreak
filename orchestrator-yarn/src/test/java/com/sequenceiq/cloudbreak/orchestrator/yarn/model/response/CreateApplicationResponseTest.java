package com.sequenceiq.cloudbreak.orchestrator.yarn.model.response;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CreateApplicationResponseTest {

    private static final String URI = "/services/v1/applications/demo-app";

    private static final String STATE = "ACCEPTED";

    @Test
    public void testUri() throws Exception {
        CreateApplicationResponse createApplicationResponse = new CreateApplicationResponse();
        createApplicationResponse.setUri(URI);
        assertEquals(URI, createApplicationResponse.getUri());
    }

    @Test
    public void testState() throws Exception {
        CreateApplicationResponse createApplicationResponse = new CreateApplicationResponse();
        createApplicationResponse.setState(STATE);
        assertEquals(STATE, createApplicationResponse.getState());
    }
}