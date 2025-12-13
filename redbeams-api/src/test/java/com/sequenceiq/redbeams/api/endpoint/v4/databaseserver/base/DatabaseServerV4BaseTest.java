package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseServerV4BaseTest {

    private DatabaseServerV4Base base;

    @BeforeEach
    public void setUp() {
        base = new DatabaseServerV4BaseTestImpl();
    }

    @Test
    void testGettersAndSetters() {
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

        base.setConnectionDriver("org.postgresql.Driver");
        assertEquals("org.postgresql.Driver", base.getConnectionDriver());

        base.setEnvironmentCrn("myenvironment");
        assertEquals("myenvironment", base.getEnvironmentCrn());

    }

    private static class DatabaseServerV4BaseTestImpl extends DatabaseServerV4Base {
    }

}
