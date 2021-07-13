package com.sequenceiq.flow.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowCheckResponse {

    private String flowId;

    private String flowChainId;

    private Boolean hasActiveFlow;

    private Boolean latestFlowFinalizedAndFailed;

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
}
