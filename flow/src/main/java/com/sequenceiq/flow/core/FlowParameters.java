package com.sequenceiq.flow.core;

import com.sequenceiq.flow.api.model.operation.OperationType;

import io.opentracing.SpanContext;

public class FlowParameters {
    private String flowId;

    private String flowTriggerUserCrn;

    private String flowOperationType;

    private SpanContext spanContext;

    public FlowParameters(String flowId, String flowTriggerUserCrn, SpanContext spanContext) {
        this.flowId = flowId;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.flowOperationType = OperationType.UNKNOWN.name();
        this.spanContext = spanContext;
    }

    public FlowParameters(String flowId, String flowTriggerUserCrn, String flowOperationType, SpanContext spanContext) {
        this.flowId = flowId;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.flowOperationType = flowOperationType;
        this.spanContext = spanContext;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowTriggerUserCrn(String flowTriggerUserCrn) {
        this.flowTriggerUserCrn = flowTriggerUserCrn;
    }

    public String getFlowTriggerUserCrn() {
        return flowTriggerUserCrn;
    }

    public SpanContext getSpanContext() {
        return spanContext;
    }

    public void setSpanContext(SpanContext spanContext) {
        this.spanContext = spanContext;
    }

    public String getFlowOperationType() {
        return flowOperationType;
    }

    public void setFlowOperationType(String flowOperationType) {
        this.flowOperationType = flowOperationType;
    }
}
