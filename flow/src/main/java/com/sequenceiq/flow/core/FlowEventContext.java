package com.sequenceiq.flow.core;

import com.sequenceiq.cloudbreak.common.event.Payload;

public class FlowEventContext {

    private final String flowChainId;

    private final String key;

    private final String flowChainType;

    private final String flowTriggerUserCrn;

    private final Payload payload;

    private String flowId;

    private String flowOperationType;

    public FlowEventContext(String flowId, String flowTriggerUserCrn, String flowOperationType, String flowChainId, String key, String flowChainType,
            Payload payload) {
        this.flowId = flowId;
        this.flowOperationType = flowOperationType;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.flowChainId = flowChainId;
        this.key = key;
        this.flowChainType = flowChainType;
        this.payload = payload;
    }

    public String getFlowChainId() {
        return flowChainId;
    }

    public String getKey() {
        return key;
    }

    public String getFlowChainType() {
        return flowChainType;
    }

    public String getFlowTriggerUserCrn() {
        return flowTriggerUserCrn;
    }

    public Payload getPayload() {
        return payload;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowOperationType() {
        return flowOperationType;
    }

    public void setFlowOperationType(String flowOperationType) {
        this.flowOperationType = flowOperationType;
    }

    public Long getResourceId() {
        return payload != null ? payload.getResourceId() : null;
    }

    @Override
    public String toString() {
        return "FlowEventContext{" +
                "key='" + key + '\'' +
                ", flowId='" + flowId + '\'' +
                ", flowChainId='" + flowChainId + '\'' +
                ", flowChainType='" + flowChainType + '\'' +
                ", flowTriggerUserCrn='" + flowTriggerUserCrn + '\'' +
                ", flowOperationType='" + flowOperationType + '\'' +
                '}';
    }
}
