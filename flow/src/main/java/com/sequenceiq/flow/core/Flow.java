package com.sequenceiq.flow.core;

import java.util.Map;

import com.sequenceiq.flow.core.config.FlowConfiguration;

public interface Flow {
    void initialize(Map<Object, Object> contextParams);

    void initialize(String stateRepresentation, Map<Object, Object> variables);

    void stop();

    boolean sendEvent(FlowEventContext flowEventContext);

    FlowState getCurrentState();

    Map<Object, Object> getVariables();

    String getFlowId();

    boolean isFlowFailed();

    void setFlowFailed(Exception exception);

    Class<? extends FlowConfiguration<?>> getFlowConfigClass();
}
