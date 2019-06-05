package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import static org.junit.Assert.assertEquals;

import com.sequenceiq.redbeams.api.endpoint.v4.database.base.DatabaseV4Identifiers;

import org.junit.Before;
import org.junit.Test;

public class DatabaseTestV4RequestTest {

    private DatabaseTestV4Request request;

    @Before
    public void setUp() {
        request = new DatabaseTestV4Request();
    }

    @Test
    public void testGettersAndSetters() {
        DatabaseV4Identifiers identifiers = new DatabaseV4Identifiers();
        request.setExistingDatabase(identifiers);
        assertEquals(identifiers, request.getExistingDatabase());

        DatabaseV4Request serverRequest = new DatabaseV4Request();
        serverRequest.setName("mydb1");
        request.setDatabase(serverRequest);
        assertEquals("mydb1", request.getDatabase().getName());

    }

}
