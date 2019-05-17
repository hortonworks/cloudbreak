package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DatabaseServerTestV4ResponseTest {

    private DatabaseServerTestV4Response response;

    @Before
    public void setUp() {
        response = new DatabaseServerTestV4Response();
    }

    @Test
    public void testGettersAndSetters() {
        response.setResult("fantastic");
        assertEquals("fantastic", response.getResult());
    }
}
