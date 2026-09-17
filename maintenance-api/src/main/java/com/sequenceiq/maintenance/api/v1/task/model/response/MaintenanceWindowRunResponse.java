package com.sequenceiq.maintenance.api.v1.task.model.response;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.RunResponse.RESPONSE)
public class MaintenanceWindowRunResponse {

    @Schema(description = ModelDescriptions.RunResponse.ID)
    private Long id;

    @Schema(description = ModelDescriptions.RunResponse.TASK_ID)
    private Long taskId;

    @Schema(description = ModelDescriptions.RunResponse.STATUS)
    private String status;

    @Schema(description = ModelDescriptions.RunResponse.ERROR_DETAIL)
    private String errorDetail;

    @Schema(description = ModelDescriptions.RunResponse.WINDOW_START)
    private Long windowStart;

    @Schema(description = ModelDescriptions.RunResponse.WINDOW_END)
    private Long windowEnd;

    @Schema(description = ModelDescriptions.RunResponse.WINDOW_EXECUTION_END)
    private Long windowExecutionEnd;

    @Schema(description = ModelDescriptions.RunResponse.ATTEMPT_COUNT)
    private int attemptCount;

    @Schema(description = ModelDescriptions.RunResponse.VERSION)
    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public Long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Long windowStart) {
        this.windowStart = windowStart;
    }

    public Long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Long getWindowExecutionEnd() {
        return windowExecutionEnd;
    }

    public void setWindowExecutionEnd(Long windowExecutionEnd) {
        this.windowExecutionEnd = windowExecutionEnd;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowRunResponse{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", status='" + status + '\'' +
                ", errorDetail='" + errorDetail + '\'' +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", windowExecutionEnd=" + windowExecutionEnd +
                ", attemptCount=" + attemptCount +
                ", version=" + version +
                '}';
    }
}
