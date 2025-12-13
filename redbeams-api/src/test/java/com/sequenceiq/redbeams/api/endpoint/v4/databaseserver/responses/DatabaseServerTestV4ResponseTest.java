package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseServerTestV4ResponseTest {

    private DatabaseServerTestV4Response response;

    @BeforeEach
    public void setUp() {
        response = new DatabaseServerTestV4Response();
    }

    @Test
    void testGettersAndSetters() {
        response.setResult("fantastic");
        assertEquals("fantastic", response.getResult());
    }
}
