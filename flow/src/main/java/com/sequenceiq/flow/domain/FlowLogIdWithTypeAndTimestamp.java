package com.sequenceiq.flow.domain;

public interface FlowLogIdWithTypeAndTimestamp {

    String getFlowId();

    ClassValue getFlowType();

    Long getCreated();
}
