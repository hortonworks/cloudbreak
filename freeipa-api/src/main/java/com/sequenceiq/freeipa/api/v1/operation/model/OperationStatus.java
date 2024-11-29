package com.sequenceiq.freeipa.api.v1.operation.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OperationV1Status")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationStatus {

    @Schema(description = UserModelDescriptions.USERSYNC_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String operationId;

    @Schema(description = UserModelDescriptions.SYNC_OPERATION, requiredMode = Schema.RequiredMode.REQUIRED)
    private OperationType operationType;

    @Schema(description = UserModelDescriptions.USERSYNC_STATUS, requiredMode = Schema.RequiredMode.REQUIRED)
    private OperationState status;

    @Schema(description = UserModelDescriptions.SUCCESS_ENVIRONMENTS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SuccessDetails> success = new ArrayList<>();

    @Schema(description = UserModelDescriptions.FAILURE_ENVIRONMENTS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FailureDetails> failure = new ArrayList<>();

    @Schema(description = UserModelDescriptions.USERSYNC_ERROR)
    private String error;

    @Schema(description = UserModelDescriptions.USERSYNC_STARTTIME)
    private Long startTime;

    @Schema(description = UserModelDescriptions.USERSYNC_ENDTIME)
    private Long endTime;

    public OperationStatus(String operationId, OperationType operationType, OperationState status,
            List<SuccessDetails> success, List<FailureDetails> failure,
            String error,
            long startTime, Long endTime) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.status = status;
        this.success = success;
        this.failure = failure;
        this.error = error;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public OperationStatus() {
    }

    public String getOperationId() {
        return operationId;
    }

    public com.sequenceiq.freeipa.api.v1.operation.model.OperationType getOperationType() {
        return operationType;
    }

    public OperationState getStatus() {
        return status;
    }

    public void setStatus(OperationState status) {
        this.status = status;
    }

    public List<SuccessDetails> getSuccess() {
        return success;
    }

    public void setSuccess(List<SuccessDetails> success) {
        this.success = success;
    }

    public List<FailureDetails> getFailure() {
        return failure;
    }

    public void setFailure(List<FailureDetails> failure) {
        this.failure = failure;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "OperationStatus{"
                + "operationId='" + operationId + '\''
                + ", operationType=" + operationType
                + ", status=" + status
                + ", success=" + success
                + ", failure=" + failure
                + ", error='" + error + '\''
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + '}';
    }
}
