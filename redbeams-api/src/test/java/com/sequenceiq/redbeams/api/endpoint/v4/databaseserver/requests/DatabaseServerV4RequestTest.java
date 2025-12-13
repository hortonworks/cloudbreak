package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseServerV4RequestTest {

    private DatabaseServerV4Request request;

    @BeforeEach
    public void setUp() {
        request = new DatabaseServerV4Request();
    }

    @Test
    void testGettersAndSetters() {
        request.setConnectionUserName("root");
        assertEquals("root", request.getConnectionUserName());

        request.setConnectionPassword("cloudera");
        assertEquals("cloudera", request.getConnectionPassword());

    }

}
