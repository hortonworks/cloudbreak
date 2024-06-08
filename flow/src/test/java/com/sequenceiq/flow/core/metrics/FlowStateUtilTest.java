package com.sequenceiq.flow.core.metrics;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.TestFlowConfig;

class FlowStateUtilTest {

    @Test
    void getFlowStateEnumShouldReturnNullWhenNextFlowStateIsNull() {
        assertNull(FlowStateUtil.getFlowStateEnum(TestFlowConfig.TestFlowState.class, null, null));
    }

    @Test
    void getFlowStateEnumShouldReturnNullWhenParsingEnumValueThrowsException() {
        assertNull(FlowStateUtil.getFlowStateEnum(TestFlowConfig.TestFlowState.class, "UNKNOWN_ENUM_VALUE", null));
    }

    @Test
    void getFlowStateEnumShouldReturnEnum() {
        Enum<? extends FlowState> flowStateEnum = FlowStateUtil.getFlowStateEnum(TestFlowConfig.TestFlowState.class,
                TestFlowConfig.TestFlowState.TEST_FINISHED_STATE.name(), null);
        assertNotNull(flowStateEnum);
        assertEquals(TestFlowConfig.TestFlowState.TEST_FINISHED_STATE, flowStateEnum);
    }
}