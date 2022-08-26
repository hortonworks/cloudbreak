package com.sequenceiq.datalake.service.sdx.poller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PollerResultTest {

    @Test
    void testSuccess() {
        PollerRunnerResult pollerRunnerResult = PollerRunnerResult.ofSuccess();

        assertTrue(pollerRunnerResult.isSuccess());
        assertNull(pollerRunnerResult.getMessage());
        assertNull(pollerRunnerResult.getException());
    }

    @Test
    void testError() {
        Exception exception = new Exception("my exception");
        PollerRunnerResult pollerRunnerResult = PollerRunnerResult.ofError(exception, "my message");

        assertFalse(pollerRunnerResult.isSuccess());
        assertEquals("my message", pollerRunnerResult.getMessage());
        assertEquals(exception, pollerRunnerResult.getException());
    }

}
