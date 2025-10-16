package com.sequenceiq.flow.core;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.api.model.operation.OperationType;

public class FlowParameters {

    private final String flowChainId;

    private final String key;

    private final String flowChainType;

    private final String flowTriggerUserCrn;

    private final Map<Object, Object> contextParams;

    @JsonIgnore
    private final Payload payload;

    private String flowId;

    private String flowOperationType;

    public FlowParameters(String flowId, String flowTriggerUserCrn) {
        this(flowId, flowTriggerUserCrn, OperationType.UNKNOWN.name(), null, null, null, new HashMap<>(), null);
    }

    @JsonCreator
    public FlowParameters(
            @JsonProperty("flowId") String flowId,
            @JsonProperty("flowTriggerUserCrn") String flowTriggerUserCrn,
            @JsonProperty("flowOperationType")  String flowOperationType,
            @JsonProperty("flowChainId")  String flowChainId,
            @JsonProperty("key") String key,
            @JsonProperty("flowChainType") String flowChainType,
            @JsonProperty("contextParams") Map<Object, Object> contextParams,
            @JsonProperty(value = "payload", required = false) Payload payload) {
        this.flowId = flowId;
        this.flowOperationType = flowOperationType;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.flowChainId = flowChainId;
        this.key = key;
        this.flowChainType = flowChainType;
        this.contextParams = contextParams == null ? new HashMap<>() : contextParams;
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

    public Map<Object, Object> getContextParams() {
        return contextParams;
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
        return "FlowParameters{" +
                "key='" + key + '\'' +
                ", flowId='" + flowId + '\'' +
                ", flowChainId='" + flowChainId + '\'' +
                ", flowChainType='" + flowChainType + '\'' +
                ", flowTriggerUserCrn='" + flowTriggerUserCrn + '\'' +
                ", flowOperationType='" + flowOperationType + '\'' +
                ", contextParams=" + contextParams +
                '}';
    }
}
