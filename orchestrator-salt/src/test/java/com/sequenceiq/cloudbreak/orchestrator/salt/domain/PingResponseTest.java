package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PingResponseTest {

    @Test
    void testGetResultByMinionIdWhenResultIsNull() {
        PingResponse pingResponse = new PingResponse();
        Map<String, Boolean> resultByMinionId = pingResponse.getResultByMinionId();
        assertNotNull(resultByMinionId);
        assertTrue(resultByMinionId.isEmpty());
    }

}