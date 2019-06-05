package com.sequenceiq.redbeams.api.endpoint.v4.database.base;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DatabaseV4BaseTest {

    private DatabaseV4Base base;

    @Before
    public void setUp() {
        base = new DatabaseV4BaseTestImpl();
    }

    @Test
    public void testGettersAndSetters() {
        base.setName("mydb");
        assertEquals("mydb", base.getName());

        base.setDescription("mine not yours");
        assertEquals("mine not yours", base.getDescription());

        base.setConnectionURL("jdbc:postgresql://myserver.db.example.com:5432/mydb");
        assertEquals("jdbc:postgresql://myserver.db.example.com:5432/mydb", base.getConnectionURL());

        base.setType("hive");
        assertEquals("hive", base.getType());

        base.setConnectorJarUrl("http://drivers.example.com/postgresql.jar");
        assertEquals("http://drivers.example.com/postgresql.jar", base.getConnectorJarUrl());

        base.setEnvironmentId("myenvironment");
        assertEquals("myenvironment", base.getEnvironmentId());

    }

    private static class DatabaseV4BaseTestImpl extends DatabaseV4Base {
    }

}
