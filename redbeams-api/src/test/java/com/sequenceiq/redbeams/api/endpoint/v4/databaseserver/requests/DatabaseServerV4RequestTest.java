package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DatabaseServerV4RequestTest {

    private DatabaseServerV4Request request;

    @Before
    public void setUp() {
        request = new DatabaseServerV4Request();
    }

    @Test
    public void testGettersAndSetters() {
        request.setConnectionUserName("root");
        assertEquals("root", request.getConnectionUserName());

        request.setConnectionPassword("cloudera");
        assertEquals("cloudera", request.getConnectionPassword());

    }

}
