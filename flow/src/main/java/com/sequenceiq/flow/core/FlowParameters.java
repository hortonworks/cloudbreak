package com.sequenceiq.flow.core;

public class FlowParameters {
    private String flowId;

    private String flowTriggerUserCrn;

    public FlowParameters(String flowId, String flowTriggerUserCrn) {
        this.flowId = flowId;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
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
}
