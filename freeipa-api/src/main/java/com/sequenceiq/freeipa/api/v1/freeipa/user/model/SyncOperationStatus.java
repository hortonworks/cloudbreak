package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SyncOperationV1Status")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncOperationStatus {

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ID, required = true)
    private final String operationId;

    @ApiModelProperty(value = UserModelDescriptions.SYNC_OPERATION, required = true)
    private final SyncOperationType syncOperationType;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_STATUS)
    private SynchronizationStatus status;

    @ApiModelProperty(value = UserModelDescriptions.SUCCESS_ENVIRONMENTNAME)
    private List<SuccessDetails> success;

    @ApiModelProperty(value = UserModelDescriptions.FAILURE_ENVIRONMENTNAME)
    private List<FailureDetails> failure;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ERROR)
    private String error;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_STARTTIME)
    private final long startTime;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ENDTIME)
    private Long endTime;

    public SyncOperationStatus(String operationId, SyncOperationType syncOperationType, SynchronizationStatus status,
            List<SuccessDetails> success, List<FailureDetails> failure,
            String error,
            long startTime, Long endTime) {
        this.operationId = operationId;
        this.syncOperationType = syncOperationType;
        this.status = status;
        this.success = success;
        this.failure = failure;
        this.error = error;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getOperationId() {
        return operationId;
    }

    public SyncOperationType getSyncOperationType() {
        return syncOperationType;
    }

    public SynchronizationStatus getStatus() {
        return status;
    }

    public void setStatus(SynchronizationStatus status) {
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

    public long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
