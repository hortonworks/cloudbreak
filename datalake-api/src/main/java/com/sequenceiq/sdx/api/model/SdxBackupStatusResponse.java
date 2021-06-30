package com.sequenceiq.sdx.api.model;

public class SdxBackupStatusResponse {

    private String operationId;

    private String status;

    private String reason;

    public SdxBackupStatusResponse() {
    }

    public SdxBackupStatusResponse(String operationId, String status, String reason) {
        this.operationId = operationId;
        this.status = status;
        this.reason = reason;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public String toString() {
        return "SdxBackupStatusResponse{" +
                "OperationId=" + operationId +
                "Status=" + status +
                "Reason=" + reason +
                '}';
    }
}
