package com.sequenceiq.freeipa.api.v1.operation.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("OperationV1Status")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationStatus {

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ID, required = true)
    private String operationId;

    @ApiModelProperty(value = UserModelDescriptions.SYNC_OPERATION, required = true)
    private OperationType operationType;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_STATUS)
    private OperationState status;

    @ApiModelProperty(value = UserModelDescriptions.SUCCESS_ENVIRONMENTS)
    private List<SuccessDetails> success;

    @ApiModelProperty(value = UserModelDescriptions.FAILURE_ENVIRONMENTS)
    private List<FailureDetails> failure;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ERROR)
    private String error;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_STARTTIME)
    private Long startTime;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ENDTIME)
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
