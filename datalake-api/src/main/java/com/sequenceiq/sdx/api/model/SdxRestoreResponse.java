package com.sequenceiq.sdx.api.model;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxRestoreResponse {

    private String operationId;

    private FlowIdentifier flowIdentifier;

    public SdxRestoreResponse() {
    }

    public SdxRestoreResponse(String operationId, FlowIdentifier flowIdentifier) {
        this.operationId = operationId;
        this.flowIdentifier = flowIdentifier;
    }

    public String getOperationId() {
        return operationId;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public String toString() {
        return "SdxDatabaseRestoreResponse{" +
                "FlowIdentifier= " + flowIdentifier.toString() +
                "OperationId=" + operationId +
                '}';
    }
}
