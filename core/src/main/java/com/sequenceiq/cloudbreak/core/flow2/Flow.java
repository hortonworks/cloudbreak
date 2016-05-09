package com.sequenceiq.cloudbreak.core.flow2;

public interface Flow {
    void initialize();
    void sendEvent(String key, Object object);
    FlowState getCurrentState();
    String getFlowId();
    void setFlowFailed();
    boolean isFlowFailed();
}
