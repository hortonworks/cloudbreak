package com.sequenceiq.environment;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.helper.FlowStateEnumChecker;

public class FlowStateEnumTest {

    @Test
    void allFlowStateIsEnum() {
        new FlowStateEnumChecker().checkFlowStateClasses();
    }
}
