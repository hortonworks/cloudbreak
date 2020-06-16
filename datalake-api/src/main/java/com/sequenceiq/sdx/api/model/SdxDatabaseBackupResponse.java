package com.sequenceiq.sdx.api.model;

public class SdxDatabaseBackupResponse {

    private String operationId;

    public SdxDatabaseBackupResponse() {
    }

    public SdxDatabaseBackupResponse(String operationId) {
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
        return "SdxDatabaseBackupResponse{" +
                "OperationId=" + operationId +
                '}';
    }
}
