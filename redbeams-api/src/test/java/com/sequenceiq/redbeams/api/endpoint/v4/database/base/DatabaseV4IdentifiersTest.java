package com.sequenceiq.redbeams.api.endpoint.v4.database.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseV4IdentifiersTest {

    private DatabaseV4Identifiers ids;

    @BeforeEach
    public void setUp() {
        ids = new DatabaseV4Identifiers();
    }

    @Test
    void testGettersAndSetters() {
        ids.setName("mydb");
        assertEquals("mydb", ids.getName());

        ids.setEnvironmentCrn("myenvironment");
        assertEquals("myenvironment", ids.getEnvironmentCrn());

    }

}
