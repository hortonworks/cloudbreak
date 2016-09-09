package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;

public interface Flow {
    void initialize();

    void sendEvent(String key, Object object);

    FlowState getCurrentState();

    String getFlowId();

    void setFlowFailed();

    boolean isFlowFailed();

    Class<? extends FlowConfiguration> getFlowConfigClass();
}
