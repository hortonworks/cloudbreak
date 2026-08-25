package com.sequenceiq.maintenance.api.v1.task.model.request;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.ModelDescriptions;
import com.sequenceiq.maintenance.api.model.MaintenanceTaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.TaskUpdateRequest.REQUEST)
public class UpdateMaintenanceWindowTaskRequest {

    @OneOfEnum(enumClass = MaintenanceTaskStatus.class, message = "Value must be one of the followings %s", fieldName = "status")
    @Schema(description = ModelDescriptions.TaskUpdateRequest.STATUS)
    private String status;

    @Min(0)
    @Schema(description = ModelDescriptions.TaskUpdateRequest.PRIORITY)
    private Integer priority;

    @Valid
    @Schema(description = ModelDescriptions.TaskUpdateRequest.DEPENDS_ON)
    private MaintenanceWindowTaskDependencyRequest dependsOn;

    @Schema(description = ModelDescriptions.TaskUpdateRequest.RETRY_WITHIN_OCCURRENCE)
    private Boolean retryWithinOccurrence;

    @Min(1)
    @Schema(description = ModelDescriptions.TaskUpdateRequest.MAX_ATTEMPTS_PER_OCCURRENCE)
    private Integer maxAttemptsPerOccurrence;

    @Min(0)
    @Schema(description = ModelDescriptions.TaskUpdateRequest.RETRY_COOLDOWN_MINUTES)
    private Integer retryCooldownMinutes;

    @Schema(description = ModelDescriptions.TaskUpdateRequest.TASK_PAYLOAD)
    private Map<String, Object> taskPayload;

    @Schema(description = ModelDescriptions.TaskUpdateRequest.EXECUTION_REF)
    private Map<String, Object> executionRef;

    public UpdateMaintenanceWindowTaskRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public MaintenanceWindowTaskDependencyRequest getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(MaintenanceWindowTaskDependencyRequest dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Boolean getRetryWithinOccurrence() {
        return retryWithinOccurrence;
    }

    public void setRetryWithinOccurrence(Boolean retryWithinOccurrence) {
        this.retryWithinOccurrence = retryWithinOccurrence;
    }

    public Integer getMaxAttemptsPerOccurrence() {
        return maxAttemptsPerOccurrence;
    }

    public void setMaxAttemptsPerOccurrence(Integer maxAttemptsPerOccurrence) {
        this.maxAttemptsPerOccurrence = maxAttemptsPerOccurrence;
    }

    public Integer getRetryCooldownMinutes() {
        return retryCooldownMinutes;
    }

    public void setRetryCooldownMinutes(Integer retryCooldownMinutes) {
        this.retryCooldownMinutes = retryCooldownMinutes;
    }

    public Map<String, Object> getTaskPayload() {
        return taskPayload;
    }

    public void setTaskPayload(Map<String, Object> taskPayload) {
        this.taskPayload = taskPayload;
    }

    public Map<String, Object> getExecutionRef() {
        return executionRef;
    }

    public void setExecutionRef(Map<String, Object> executionRef) {
        this.executionRef = executionRef;
    }

    @AssertTrue(message = "status must be DISABLED when provided on update")
    public boolean isSupportedStatusUpdate() {
        return status == null || MaintenanceTaskStatus.DISABLED.name().equals(status);
    }
}
