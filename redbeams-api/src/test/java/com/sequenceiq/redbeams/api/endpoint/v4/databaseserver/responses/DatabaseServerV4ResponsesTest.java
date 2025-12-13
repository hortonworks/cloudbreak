package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class DatabaseServerV4ResponsesTest {

    private DatabaseServerV4Responses responses;

    @Test
    void testEmpty() {
        responses = new DatabaseServerV4Responses();
        assertEquals(0, responses.getResponses().size());
    }

    @Test
    void testNonEmpty() {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setId(1L);

        responses = new DatabaseServerV4Responses(Collections.singleton(response));
        assertEquals(1, responses.getResponses().size());
        assertEquals(1L, responses.getResponses().iterator().next().getId().longValue());
    }
}
