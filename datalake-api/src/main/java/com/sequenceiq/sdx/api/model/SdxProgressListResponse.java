package com.sequenceiq.sdx.api.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxProgressListResponse implements Serializable {

    @ApiModelProperty(ModelDescriptions.RECENT_FLOW_OPERATIONS)
    private List<FlowProgressResponse> recentFlowOperations;

    @ApiModelProperty(ModelDescriptions.RECENT_INTERNAL_FLOW_OPERATIONS)
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
