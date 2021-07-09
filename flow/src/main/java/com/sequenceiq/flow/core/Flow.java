package com.sequenceiq.flow.core;

import java.util.Map;

import com.sequenceiq.flow.core.config.FlowConfiguration;

import io.opentracing.SpanContext;

public interface Flow {
    void initialize(Map<Object, Object> contextParams);

    void initialize(String stateRepresentation, Map<Object, Object> variables);

    void stop();

    void sendEvent(String key, String flowTriggerUserCrn, Object object,
            SpanContext spanContext, String operationType);

    FlowState getCurrentState();

    Map<Object, Object> getVariables();

    String getFlowId();

    void setFlowFailed(Exception exception);

    boolean isFlowFailed();

    Class<? extends FlowConfiguration<?>> getFlowConfigClass();
}
