package com.sequenceiq.cloudbreak.orchestrator.yarn.model.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CreateApplicationResponseTest {

    private static final String URI = "/services/v1/applications/demo-app";

    private static final String STATE = "ACCEPTED";

    @Test
    void testUri() {
        CreateApplicationResponse createApplicationResponse = new CreateApplicationResponse();
        createApplicationResponse.setUri(URI);
        assertEquals(URI, createApplicationResponse.getUri());
    }

    @Test
    void testState() {
        CreateApplicationResponse createApplicationResponse = new CreateApplicationResponse();
        createApplicationResponse.setState(STATE);
        assertEquals(STATE, createApplicationResponse.getState());
    }
}