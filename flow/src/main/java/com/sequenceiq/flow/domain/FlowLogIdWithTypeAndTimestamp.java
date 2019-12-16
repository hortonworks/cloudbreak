package com.sequenceiq.flow.domain;

public interface FlowLogIdWithTypeAndTimestamp {

    String getFlowId();

    Class<?> getFlowType();

    Long getCreated();
}
