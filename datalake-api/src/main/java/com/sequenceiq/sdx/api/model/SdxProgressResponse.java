package com.sequenceiq.sdx.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxProgressResponse implements Serializable {

    @Schema(description = ModelDescriptions.LAST_FLOW_OPERATION)
    private FlowProgressResponse lastFlowOperation;

    @Schema(description = ModelDescriptions.LAST_INTERNAL_FLOW_OPERATION)
    private FlowProgressResponse lastInternalFlowOperation;

    public FlowProgressResponse getLastFlowOperation() {
        return lastFlowOperation;
    }

    public void setLastFlowOperation(FlowProgressResponse lastFlowOperation) {
        this.lastFlowOperation = lastFlowOperation;
    }

    public FlowProgressResponse getLastInternalFlowOperation() {
        return lastInternalFlowOperation;
    }

    public void setLastInternalFlowOperation(FlowProgressResponse lastInternalFlowOperation) {
        this.lastInternalFlowOperation = lastInternalFlowOperation;
    }
}
