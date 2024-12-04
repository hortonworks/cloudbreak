package com.sequenceiq.flow.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowLogResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long resourceId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long created;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String flowId;

    private String flowChainId;

    private String nextEvent;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentState;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean finalized;

    private String nodeId;

    private StateStatus stateStatus;

    private String flowTriggerUserCrn;

    private Long endTime;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowChainId() {
        return flowChainId;
    }

    public void setFlowChainId(String flowChainId) {
        this.flowChainId = flowChainId;
    }

    public String getNextEvent() {
        return nextEvent;
    }

    public void setNextEvent(String nextEvent) {
        this.nextEvent = nextEvent;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public StateStatus getStateStatus() {
        return stateStatus;
    }

    public void setStateStatus(StateStatus stateStatus) {
        this.stateStatus = stateStatus;
    }

    public String getFlowTriggerUserCrn() {
        return flowTriggerUserCrn;
    }

    public void setFlowTriggerUserCrn(String flowTriggerUserCrn) {
        this.flowTriggerUserCrn = flowTriggerUserCrn;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "FlowLogResponse{" +
                "resourceId=" + resourceId +
                ", created=" + created +
                ", flowId='" + flowId + '\'' +
                ", flowChainId='" + flowChainId + '\'' +
                ", nextEvent='" + nextEvent + '\'' +
                ", currentState='" + currentState + '\'' +
                ", finalized=" + finalized +
                ", nodeId='" + nodeId + '\'' +
                ", stateStatus=" + stateStatus +
                ", flowTriggerUserCrn='" + flowTriggerUserCrn + '\'' +
                ", flowEndTime='" + endTime + '\'' +
                '}';
    }
}
