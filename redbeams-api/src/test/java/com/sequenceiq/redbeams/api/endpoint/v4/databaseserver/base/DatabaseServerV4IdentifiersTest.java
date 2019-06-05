package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DatabaseServerV4IdentifiersTest {

    private DatabaseServerV4Identifiers ids;

    @Before
    public void setUp() {
        ids = new DatabaseServerV4Identifiers();
    }

    @Test
    public void testGettersAndSetters() {
        ids.setName("myserver");
        assertEquals("myserver", ids.getName());

        ids.setEnvironmentId("myenvironment");
        assertEquals("myenvironment", ids.getEnvironmentId());

    }

}
