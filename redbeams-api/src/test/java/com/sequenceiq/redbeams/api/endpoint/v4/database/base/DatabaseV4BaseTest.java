package com.sequenceiq.redbeams.api.endpoint.v4.database.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseV4BaseTest {

    private DatabaseV4Base base;

    @BeforeEach
    public void setUp() {
        base = new DatabaseV4BaseTestImpl();
    }

    @Test
    void testGettersAndSetters() {
        base.setName("mydb");
        assertEquals("mydb", base.getName());

        base.setDescription("mine not yours");
        assertEquals("mine not yours", base.getDescription());

        base.setConnectionURL("jdbc:postgresql://myserver.db.example.com:5432/mydb");
        assertEquals("jdbc:postgresql://myserver.db.example.com:5432/mydb", base.getConnectionURL());

        base.setType("hive");
        assertEquals("hive", base.getType());

        base.setConnectionDriver("org.postgresql.Driver");
        assertEquals("org.postgresql.Driver", base.getConnectionDriver());

        base.setEnvironmentCrn("myenvironment");
        assertEquals("myenvironment", base.getEnvironmentCrn());

    }

    private static class DatabaseV4BaseTestImpl extends DatabaseV4Base {
    }

}
