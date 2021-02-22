package com.sequenceiq.sdx.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SdxProgressResponse implements Serializable {

    private FlowProgressResponse lastFlowOperation;

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
