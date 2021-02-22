package com.sequenceiq.sdx.api.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SdxProgressListResponse implements Serializable {

    private List<FlowProgressResponse> recentFlowOperations;

    private List<FlowProgressResponse> recentInternalFlowOperations;

    public List<FlowProgressResponse> getRecentFlowOperations() {
        return recentFlowOperations;
    }

    public void setRecentFlowOperations(List<FlowProgressResponse> recentFlowOperations) {
        this.recentFlowOperations = recentFlowOperations;
    }

    public List<FlowProgressResponse> getRecentInternalFlowOperations() {
        return recentInternalFlowOperations;
    }

    public void setRecentInternalFlowOperations(List<FlowProgressResponse> recentInternalFlowOperations) {
        this.recentInternalFlowOperations = recentInternalFlowOperations;
    }
}
