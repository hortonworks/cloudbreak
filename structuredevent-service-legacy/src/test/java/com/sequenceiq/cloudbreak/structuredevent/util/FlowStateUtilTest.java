package com.sequenceiq.cloudbreak.structuredevent.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.TestFlowState;
import com.sequenceiq.flow.core.FlowState;

class FlowStateUtilTest {

    @Test
    void getFlowStateEnumShouldReturnNullWhenFlowDetailsIsNull() {
        assertNull(FlowStateUtil.getFlowStateEnum(TestFlowState.class, null));
    }

    @Test
    void getFlowStateEnumShouldReturnNullWhenNextFlowStateIsNull() {
        assertNull(FlowStateUtil.getFlowStateEnum(TestFlowState.class, new FlowDetails()));
    }

    @Test
    void getFlowStateEnumShouldReturnNullWhenParsingEnumValueThrowsException() {
        assertNull(FlowStateUtil.getFlowStateEnum(TestFlowState.class, flowDetails("UNKNOWN_ENUM_VALUE")));
    }

    @Test
    void getFlowStateEnumShouldReturnEnum() {
        Enum<? extends FlowState> flowStateEnum = FlowStateUtil.getFlowStateEnum(TestFlowState.class, flowDetails(TestFlowState.FINISHED_STATE.name()));
        assertNotNull(flowStateEnum);
        assertEquals(TestFlowState.FINISHED_STATE, flowStateEnum);
    }

    private FlowDetails flowDetails(String nextFlowState) {
        return new FlowDetails(null, null, null, null, null, nextFlowState, null, 0L);
    }

}