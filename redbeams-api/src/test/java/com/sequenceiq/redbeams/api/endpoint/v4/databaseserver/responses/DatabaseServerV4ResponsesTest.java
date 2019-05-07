package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

public class DatabaseServerV4ResponsesTest {

    private DatabaseServerV4Responses responses;

    @Test
    public void testEmpty() {
        responses = new DatabaseServerV4Responses();
        assertEquals(0, responses.getResponses().size());
    }

    @Test
    public void testNonEmpty() {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setId(1L);

        responses = new DatabaseServerV4Responses(Collections.singleton(response));
        assertEquals(1, responses.getResponses().size());
        assertEquals(1L, responses.getResponses().iterator().next().getId().longValue());
    }
}
