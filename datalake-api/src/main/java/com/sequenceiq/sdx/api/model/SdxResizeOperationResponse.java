package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxResizeOperationResponse {

    @Schema(description = ModelDescriptions.OPERATION_ID)
    private String operationId;

    @Schema(description = ModelDescriptions.OPERATION_ACTIVE)
    private Boolean active;

    @Schema(description = ModelDescriptions.OPERATION_FAILED)
    private Boolean failed;

    @Schema(description = ModelDescriptions.OPERATION_STATUS_REASON)
    private String statusReason;

    @Schema(description = ModelDescriptions.RESIZE_ROLLBACK)
    private Boolean rollbackAllowed;

    @Schema(description = ModelDescriptions.RESIZE_RETRY)
    private Boolean retryAllowed;

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Boolean getRollbackAllowed() {
        return rollbackAllowed;
    }

    public void setRollbackAllowed(Boolean rollbackAllowed) {
        this.rollbackAllowed = rollbackAllowed;
    }

    public Boolean getRetryAllowed() {
        return retryAllowed;
    }

    public void setRetryAllowed(Boolean retryAllowed) {
        this.retryAllowed = retryAllowed;
    }

    @Override
    public String toString() {
        return "SdxResizeOperation{" +
                "operationId='" + operationId + '\'' +
                ", active=" + active +
                ", failed=" + failed +
                ", statusReason='" + statusReason + '\'' +
                ", rollbackAllowed=" + rollbackAllowed +
                ", retryAllowed=" + retryAllowed +
                '}';
    }
}
