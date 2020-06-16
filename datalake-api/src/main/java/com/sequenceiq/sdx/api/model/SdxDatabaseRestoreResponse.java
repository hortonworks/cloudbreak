package com.sequenceiq.sdx.api.model;

public class SdxDatabaseRestoreResponse {
    private String operationId;

    public SdxDatabaseRestoreResponse() {
    }

    public SdxDatabaseRestoreResponse(String operationId) {
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public String toString() {
        return "SdxDatabaseRestoreResponse{" +
                "OperationId=" + operationId +
                '}';
    }
}
