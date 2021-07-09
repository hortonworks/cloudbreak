package com.sequenceiq.flow.api.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowProgressResponse implements Serializable {

    private String flowId;

    private String flowChainId;

    private String resourceCrn;

    private Long created;

    private List<FlowStateTransitionResponse> transitions;

    private Integer progress;

    private Double elapsedTimeInSeconds;

    private Boolean finalized;

    private Integer maxNumberOfTransitions;

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public List<FlowStateTransitionResponse> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<FlowStateTransitionResponse> transitions) {
        this.transitions = transitions;
    }

    public Double getElapsedTimeInSeconds() {
        return elapsedTimeInSeconds;
    }

    public void setElapsedTimeInSeconds(Double elapsedTimeInSeconds) {
        this.elapsedTimeInSeconds = elapsedTimeInSeconds;
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

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Integer getMaxNumberOfTransitions() {
        return maxNumberOfTransitions;
    }

    public void setMaxNumberOfTransitions(Integer maxNumberOfTransitions) {
        this.maxNumberOfTransitions = maxNumberOfTransitions;
    }
}
