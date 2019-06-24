package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class TerminateDatabaseServerV4RequestTest {

    private TerminateDatabaseServerV4Request request;

    @Before
    public void setUp() throws Exception {
        request = new TerminateDatabaseServerV4Request();
    }

    @Test
    public void testGettersAndSetters() {
        request.setName("myallocation");
        assertEquals("myallocation", request.getName());

        request.setEnvironmentId("myenv");
        assertEquals("myenv", request.getEnvironmentId());
    }

}
