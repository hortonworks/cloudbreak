package com.sequenceiq.flow.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowCheckResponse {

    private String flowId;

    private String flowChainId;

    private Boolean hasActiveFlow;

    private Boolean latestFlowFinalizedAndFailed;

    private Long endTime;

    private String currentState;

    private String nextEvent;

    private String flowType;

    private String reason;

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

    public Boolean getHasActiveFlow() {
        return hasActiveFlow;
    }

    public void setHasActiveFlow(Boolean hasActiveFlow) {
        this.hasActiveFlow = hasActiveFlow;
    }

    public Boolean getLatestFlowFinalizedAndFailed() {
        return latestFlowFinalizedAndFailed;
    }

    public void setLatestFlowFinalizedAndFailed(Boolean latestFlowFinalizedAndFailed) {
        this.latestFlowFinalizedAndFailed = latestFlowFinalizedAndFailed;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentFailedState) {
        this.currentState = currentFailedState;
    }

    public String getNextEvent() {
        return nextEvent;
    }

    public void setNextEvent(String nextEvent) {
        this.nextEvent = nextEvent;
    }

    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String toString() {
        return "FlowCheckResponse{" +
                "flowId='" + flowId + '\'' +
                ", flowChainId='" + flowChainId + '\'' +
                ", hasActiveFlow='" + hasActiveFlow + '\'' +
                ", latestFlowFinalizedAndFailed='" + latestFlowFinalizedAndFailed + '\'' +
                ", endTime='" + endTime + '\'' +
                ", currentState='" + currentState + '\'' +
                ", nextEvent=" + nextEvent + '\'' +
                ", flowType=" + flowType + '\'' +
                ", reason=" + reason + '\'' +
                '}';
    }
}
