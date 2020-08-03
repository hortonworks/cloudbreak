package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowDetails implements Serializable {
    private String flowChainType;

    private String flowType;

    private String flowChainId;

    private String flowId;

    private String flowState;

    private String nextFlowState;

    private String flowEvent;

    private Long duration;

    public FlowDetails() {
    }

    public FlowDetails(String flowChainType, String flowType, String flowChainId, String flowId, String flowState, String nextFlowState, String flowEvent,
            Long duration) {
        this.flowChainType = flowChainType;
        this.flowType = flowType;
        this.flowChainId = flowChainId;
        this.flowId = flowId;
        this.flowState = flowState;
        this.nextFlowState = nextFlowState;
        this.flowEvent = flowEvent;
        this.duration = duration;
    }

    public String getFlowChainType() {
        return flowChainType;
    }

    public String getFlowType() {
        return flowType;
    }

    public String getFlowChainId() {
        return flowChainId;
    }

    public String getFlowId() {
        return flowId;
    }

    public String getFlowState() {
        return flowState;
    }

    public String getNextFlowState() {
        return nextFlowState;
    }

    public String getFlowEvent() {
        return flowEvent;
    }

    public Long getDuration() {
        return duration;
    }

    public void setFlowChainType(String flowChainType) {
        this.flowChainType = flowChainType;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public void setFlowChainId(String flowChainId) {
        this.flowChainId = flowChainId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public void setFlowState(String flowState) {
        this.flowState = flowState;
    }

    public void setNextFlowState(String nextFlowState) {
        this.nextFlowState = nextFlowState;
    }

    public void setFlowEvent(String flowEvent) {
        this.flowEvent = flowEvent;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
