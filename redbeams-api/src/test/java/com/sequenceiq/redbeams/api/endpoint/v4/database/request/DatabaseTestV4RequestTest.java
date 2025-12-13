package com.sequenceiq.redbeams.api.endpoint.v4.database.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.redbeams.api.endpoint.v4.database.base.DatabaseV4Identifiers;

class DatabaseTestV4RequestTest {

    private DatabaseTestV4Request request;

    @BeforeEach
    public void setUp() {
        request = new DatabaseTestV4Request();
    }

    @Test
    void testGettersAndSetters() {
        DatabaseV4Identifiers identifiers = new DatabaseV4Identifiers();
        request.setExistingDatabase(identifiers);
        assertEquals(identifiers, request.getExistingDatabase());

        DatabaseV4Request serverRequest = new DatabaseV4Request();
        serverRequest.setName("mydb1");
        request.setDatabase(serverRequest);
        assertEquals("mydb1", request.getDatabase().getName());

    }

}
