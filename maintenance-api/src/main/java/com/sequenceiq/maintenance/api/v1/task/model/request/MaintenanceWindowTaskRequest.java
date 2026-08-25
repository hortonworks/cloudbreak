package com.sequenceiq.maintenance.api.v1.task.model.request;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.ModelDescriptions;
import com.sequenceiq.maintenance.api.model.MaintenanceTaskKind;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.TaskRequest.REQUEST)
public class MaintenanceWindowTaskRequest {

    @NotBlank
    @Schema(description = ModelDescriptions.TaskRequest.RESOURCE_CRN)
    private String resourceCrn;

    @NotBlank
    @Schema(description = ModelDescriptions.TaskRequest.ENVIRONMENT_CRN)
    private String environmentCrn;

    @NotBlank
    @Schema(description = ModelDescriptions.TaskRequest.TASK_TYPE)
    private String taskType;

    @NotBlank
    @Schema(description = ModelDescriptions.TaskRequest.WORK_ITEM_ID)
    private String workItemId;

    @NotBlank
    @OneOfEnum(enumClass = MaintenanceTaskKind.class, message = "Value must be one of the followings %s", fieldName = "taskKind")
    @Schema(description = ModelDescriptions.TaskRequest.TASK_KIND)
    private String taskKind;

    @NotBlank
    @Schema(description = ModelDescriptions.TaskRequest.SUBMITTER_SERVICE)
    private String submitterService;

    @Schema(description = ModelDescriptions.TaskRequest.TASK_PAYLOAD)
    private Map<String, Object> taskPayload;

    @NotNull
    @Schema(description = ModelDescriptions.TaskRequest.EXECUTION_REF)
    private Map<String, Object> executionRef;

    @Min(0)
    @Schema(description = ModelDescriptions.TaskRequest.PRIORITY)
    private Integer priority;

    @Valid
    @Schema(description = ModelDescriptions.TaskRequest.DEPENDS_ON)
    private MaintenanceWindowTaskDependencyRequest dependsOn;

    @Schema(description = ModelDescriptions.TaskRequest.RETRY_WITHIN_OCCURRENCE)
    private Boolean retryWithinOccurrence;

    @Min(1)
    @Schema(description = ModelDescriptions.TaskRequest.MAX_ATTEMPTS_PER_OCCURRENCE)
    private Integer maxAttemptsPerOccurrence;

    @Min(0)
    @Schema(description = ModelDescriptions.TaskRequest.RETRY_COOLDOWN_MINUTES)
    private Integer retryCooldownMinutes;

    public MaintenanceWindowTaskRequest() {
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public void setWorkItemId(String workItemId) {
        this.workItemId = workItemId;
    }

    public String getTaskKind() {
        return taskKind;
    }

    public void setTaskKind(String taskKind) {
        this.taskKind = taskKind;
    }

    public String getSubmitterService() {
        return submitterService;
    }

    public void setSubmitterService(String submitterService) {
        this.submitterService = submitterService;
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
}
