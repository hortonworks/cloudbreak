package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRestoreStatusResponse {

    @Schema(description = ModelDescriptions.OPERATION_ID)
    private String operationId;

    @Schema(description = ModelDescriptions.OPERATION_STATUS)
    private String status;

    @Schema(description = ModelDescriptions.OPERATION_STATUS_REASON)
    private String reason;

    public SdxRestoreStatusResponse() {
    }

    public SdxRestoreStatusResponse(String operationId, String status, String reason) {
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
        return "SdxDatabaseRestoreResponse{" + "OperationId=" + operationId + "Status=" + status + "Reason=" + reason + '}';
    }
}
