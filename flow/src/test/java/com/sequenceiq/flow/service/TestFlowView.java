package com.sequenceiq.flow.service;

import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;

public class TestFlowView implements FlowLogIdWithTypeAndTimestamp {

    private final Class<?> flowType;

    public TestFlowView(Class<?> flowType) {
        this.flowType = flowType;
    }

    @Override
    public String getFlowId() {
        return "1";
    }

    @Override
    public ClassValue getFlowType() {
        return ClassValue.of(flowType);
    }

    @Override
    public Long getCreated() {
        return 1L;
    }

    @Override
    public String toString() {
        return "TestFlowView{" +
                "flowType=" + flowType +
                '}';
    }
}
