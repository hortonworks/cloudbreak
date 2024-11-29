package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SyncOperationV1Status")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncOperationStatus {

    @Schema(description = UserModelDescriptions.USERSYNC_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private final String operationId;

    @Schema(description = UserModelDescriptions.SYNC_OPERATION, requiredMode = Schema.RequiredMode.REQUIRED)
    private final SyncOperationType syncOperationType;

    @Schema(description = UserModelDescriptions.USERSYNC_STATUS, requiredMode = Schema.RequiredMode.REQUIRED)
    private SynchronizationStatus status;

    @Schema(description = UserModelDescriptions.SUCCESS_ENVIRONMENTS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SuccessDetails> success;

    @Schema(description = UserModelDescriptions.FAILURE_ENVIRONMENTS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FailureDetails> failure;

    @Schema(description = UserModelDescriptions.USERSYNC_ERROR)
    private String error;

    @Schema(description = UserModelDescriptions.USERSYNC_STARTTIME, requiredMode = Schema.RequiredMode.REQUIRED)
    private final long startTime;

    @Schema(description = UserModelDescriptions.USERSYNC_ENDTIME)
    private Long endTime;

    @JsonCreator
    public SyncOperationStatus(@JsonProperty(value = "operationId", required = true) String operationId,
            @JsonProperty(value = "syncOperationType", required = true) SyncOperationType syncOperationType,
            @JsonProperty("status") SynchronizationStatus status,
            @JsonProperty("success") List<SuccessDetails> success,
            @JsonProperty("failure") List<FailureDetails> failure,
            @JsonProperty("error") String error,
            @JsonProperty(value = "startTime", required = true) long startTime,
            @JsonProperty("endTime") Long endTime) {
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

    @Override
    public String toString() {
        return "SyncOperationStatus{"
                + "operationId='" + operationId + '\''
                + ", syncOperationType=" + syncOperationType
                + ", status=" + status
                + ", success=" + success
                + ", failure=" + failure
                + ", error='" + error + '\''
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + '}';
    }
}
