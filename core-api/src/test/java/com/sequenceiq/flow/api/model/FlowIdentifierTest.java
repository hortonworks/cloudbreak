package com.sequenceiq.flow.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FlowIdentifierTest {

    @Test
    void throwExceptionWhenFlowTypeIsFlowAndIdIsEmpty() {
        NullPointerException result1 = assertThrows(NullPointerException.class, () -> new FlowIdentifier(FlowType.FLOW, null));

        assertEquals("pollableId must not be empty", result1.getMessage());

        IllegalArgumentException result2 = assertThrows(IllegalArgumentException.class, () -> new FlowIdentifier(FlowType.FLOW, ""));

        assertEquals("pollableId must not be empty", result2.getMessage());
    }

    @Test
    void throwExceptionWhenFlowTypeIsNotTriggeredAndPollableIdIsPresent() {
        IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> new FlowIdentifier(FlowType.NOT_TRIGGERED, "FLOW_ID"));

        assertEquals("Should not set pollable id when flow type is NOT_TRIGGERED", result.getMessage());
    }
}