package com.sequenceiq.sdx.api.model;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxBackupResponse {

    private String operationId;

    private FlowIdentifier flowIdentifier;

    public SdxBackupResponse() {
    }

    public SdxBackupResponse(String operationId, FlowIdentifier flowIdentifier) {
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
        return "SdxBackupResponse{" +
                "FlowIdentifier= " + flowIdentifier.toString() +
                "OperationId=" + operationId +
                '}';
    }
}
