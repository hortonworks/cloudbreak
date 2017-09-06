package com.sequenceiq.cloudbreak.structuredevent.event;

public class FlowDetails {
    private String flowChainType;

    private String flowType;

    private String flowChainId;

    private String flowId;

    private String flowState;

    private String nextFlowState;

    private String flowEvent;

    private Long duration;

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
}
