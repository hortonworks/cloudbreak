package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.Assert.assertEquals;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base.DatabaseServerV4Identifiers;

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
        DatabaseServerV4Identifiers identifiers = new DatabaseServerV4Identifiers();
        request.setExistingDatabaseServer(identifiers);
        assertEquals(identifiers, request.getExistingDatabaseServer());

        DatabaseServerV4Request serverRequest = new DatabaseServerV4Request();
        serverRequest.setName("mydb1");
        request.setDatabaseServer(serverRequest);
        assertEquals("mydb1", request.getDatabaseServer().getName());

    }

}
