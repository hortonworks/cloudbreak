package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DatabaseServerTestV4RequestTest {

    private DatabaseServerTestV4Request request;

    @Before
    public void setUp() {
        request = new DatabaseServerTestV4Request();
    }

    @Test
    public void testGettersAndSetters() {
        request.setExistingDatabaseServerName("mydb1");
        assertEquals("mydb1", request.getExistingDatabaseServerName());

        DatabaseServerV4Request serverRequest = new DatabaseServerV4Request();
        serverRequest.setName("mydb1");
        request.setDatabaseServer(serverRequest);
        assertEquals("mydb1", request.getDatabaseServer().getName());

    }

}
