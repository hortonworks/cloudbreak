package com.sequenceiq.redbeams.api.endpoint.v4.database.base;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DatabaseV4IdentifiersTest {

    private DatabaseV4Identifiers ids;

    @Before
    public void setUp() {
        ids = new DatabaseV4Identifiers();
    }

    @Test
    public void testGettersAndSetters() {
        ids.setName("mydb");
        assertEquals("mydb", ids.getName());

        ids.setEnvironmentId("myenvironment");
        assertEquals("myenvironment", ids.getEnvironmentId());

    }

}
