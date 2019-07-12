package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DatabaseServerV4BaseTest {

    private DatabaseServerV4Base base;

    @Before
    public void setUp() {
        base = new DatabaseServerV4BaseTestImpl();
    }

    @Test
    public void testGettersAndSetters() {
        base.setName("myserver");
        assertEquals("myserver", base.getName());

        base.setDescription("mine not yours");
        assertEquals("mine not yours", base.getDescription());

        base.setHost("myserver.db.example.com");
        assertEquals("myserver.db.example.com", base.getHost());

        base.setPort(5432);
        assertEquals(5432, base.getPort().intValue());

        base.setDatabaseVendor("postgres");
        assertEquals("postgres", base.getDatabaseVendor());

        base.setConnectorJarUrl("http://drivers.example.com/postgresql.jar");
        assertEquals("http://drivers.example.com/postgresql.jar", base.getConnectorJarUrl());

        base.setEnvironmentCrn("myenvironment");
        assertEquals("myenvironment", base.getEnvironmentCrn());

    }

    private static class DatabaseServerV4BaseTestImpl extends DatabaseServerV4Base {
    }

}
