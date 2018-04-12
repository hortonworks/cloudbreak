package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;

import java.util.Map;

public interface Flow {
    void initialize();

    void initialize(String stateRepresentation, Map<Object, Object> variables);

    void stop();

    void sendEvent(String key, Object object);

    FlowState getCurrentState();

    Map<Object, Object> getVariables();

    String getFlowId();

    void setFlowFailed(Exception exception);

    boolean isFlowFailed();

    Class<? extends FlowConfiguration<?>> getFlowConfigClass();
}
