package com.sequenceiq.flow.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class FlowIdentifierTest {

    @Test
    public void throwExceptionWhenFlowTypeIsFlowAndIdIsEmpty() {
        NullPointerException result1 = Assertions.assertThrows(NullPointerException.class, () -> new FlowIdentifier(FlowType.FLOW, null));

        assertEquals("pollableId must not be empty", result1.getMessage());

        IllegalArgumentException result2 = Assertions.assertThrows(IllegalArgumentException.class, () -> new FlowIdentifier(FlowType.FLOW, ""));

        assertEquals("pollableId must not be empty", result2.getMessage());
    }

    @Test
    public void throwExceptionWhenFlowTypeIsNotTriggeredAndPollableIdIsPresent() {
        IllegalArgumentException result = Assertions.assertThrows(IllegalArgumentException.class, () -> new FlowIdentifier(FlowType.NOT_TRIGGERED, "FLOW_ID"));

        assertEquals("Should not set pollable id when flow type is NOT_TRIGGERED", result.getMessage());
    }
}