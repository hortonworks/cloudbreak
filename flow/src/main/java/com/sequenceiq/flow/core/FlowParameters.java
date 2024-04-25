package com.sequenceiq.flow.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.api.model.operation.OperationType;

public class FlowParameters {
    private String flowId;

    private String flowTriggerUserCrn;

    private String flowOperationType;

    public FlowParameters(String flowId, String flowTriggerUserCrn) {
        this.flowId = flowId;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.flowOperationType = OperationType.UNKNOWN.name();
    }

    @JsonCreator
    public FlowParameters(
            @JsonProperty("flowId") String flowId,
            @JsonProperty("flowTriggerUserCrn") String flowTriggerUserCrn,
            @JsonProperty("flowOperationType")  String flowOperationType) {

        this.flowId = flowId;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.flowOperationType = flowOperationType;
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

    public String getFlowOperationType() {
        return flowOperationType;
    }

    public void setFlowOperationType(String flowOperationType) {
        this.flowOperationType = flowOperationType;
    }

    @Override
    public String toString() {
        return "FlowParameters{" +
                "flowId='" + flowId + '\'' +
                ", flowTriggerUserCrn='" + flowTriggerUserCrn + '\'' +
                ", flowOperationType='" + flowOperationType + '\'' +
                '}';
    }
}
