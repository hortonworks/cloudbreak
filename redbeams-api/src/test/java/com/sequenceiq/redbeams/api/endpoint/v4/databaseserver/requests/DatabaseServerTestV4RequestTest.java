package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseServerTestV4RequestTest {

    private DatabaseServerTestV4Request request;

    @BeforeEach
    public void setUp() {
        request = new DatabaseServerTestV4Request();
    }

    @Test
    void testGettersAndSetters() {
        request.setExistingDatabaseServerCrn("crn");
        assertEquals("crn", request.getExistingDatabaseServerCrn());

        DatabaseServerV4Request serverRequest = new DatabaseServerV4Request();
        serverRequest.setName("mydb1");
        request.setDatabaseServer(serverRequest);
        assertEquals("mydb1", request.getDatabaseServer().getName());

    }

}
