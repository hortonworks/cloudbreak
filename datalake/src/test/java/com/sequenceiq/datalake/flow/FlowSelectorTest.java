package com.sequenceiq.datalake.flow;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.event.FlowSelectors;

class FlowSelectorTest {

    @Test
    void testDupicateHandlerDoesNotExist() {
        FlowSelectors flowSelectors = new FlowSelectors();
        flowSelectors.assertUniquenessInFlowEventNames("com.sequenceiq.datalake.flow");
    }

}
