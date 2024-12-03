package com.sequenceiq.sdx.api.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxProgressListResponse implements Serializable {

    @Schema(description = ModelDescriptions.RECENT_FLOW_OPERATIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FlowProgressResponse> recentFlowOperations = new ArrayList<>();

    @Schema(description = ModelDescriptions.RECENT_INTERNAL_FLOW_OPERATIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FlowProgressResponse> recentInternalFlowOperations = new ArrayList<>();

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
