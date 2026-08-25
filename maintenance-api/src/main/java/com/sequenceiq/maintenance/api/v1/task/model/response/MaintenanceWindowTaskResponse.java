package com.sequenceiq.maintenance.api.v1.task.model.response;

import java.util.Map;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.TaskResponse.RESPONSE)
public class MaintenanceWindowTaskResponse {

    @Schema(description = ModelDescriptions.TaskResponse.ID)
    private Long id;

    @Schema(description = ModelDescriptions.TaskResponse.ACCOUNT_ID)
    private String accountId;

    @Schema(description = ModelDescriptions.TaskResponse.RESOURCE_CRN)
    private String resourceCrn;

    @Schema(description = ModelDescriptions.TaskResponse.ENVIRONMENT_CRN)
    private String environmentCrn;

    @Schema(description = ModelDescriptions.TaskResponse.TASK_TYPE)
    private String taskType;

    @Schema(description = ModelDescriptions.TaskResponse.WORK_ITEM_ID)
    private String workItemId;

    @Schema(description = ModelDescriptions.TaskResponse.TASK_KIND)
    private String taskKind;

    @Schema(description = ModelDescriptions.TaskResponse.STATUS)
    private String status;

    @Schema(description = ModelDescriptions.TaskResponse.SUBMITTER_SERVICE)
    private String submitterService;

    @Schema(description = ModelDescriptions.TaskResponse.TASK_PAYLOAD)
    private Map<String, Object> taskPayload;

    @Schema(description = ModelDescriptions.TaskResponse.EXECUTION_REF)
    private Map<String, Object> executionRef;

    @Schema(description = ModelDescriptions.TaskResponse.PRIORITY)
    private Integer priority;

    @Schema(description = ModelDescriptions.TaskResponse.DEPENDS_ON)
    private MaintenanceWindowTaskDependencyResponse dependsOn;

    @Schema(description = ModelDescriptions.TaskResponse.RETRY_WITHIN_OCCURRENCE)
    private boolean retryWithinOccurrence;

    @Schema(description = ModelDescriptions.TaskResponse.MAX_ATTEMPTS_PER_OCCURRENCE)
    private Integer maxAttemptsPerOccurrence;

    @Schema(description = ModelDescriptions.TaskResponse.RETRY_COOLDOWN_MINUTES)
    private Integer retryCooldownMinutes;

    @Schema(description = ModelDescriptions.TaskResponse.CREATED_AT)
    private Long createdAt;

    @Schema(description = ModelDescriptions.TaskResponse.UPDATED_AT)
    private Long updatedAt;

    @Schema(description = ModelDescriptions.TaskResponse.CREATED_BY)
    private String createdBy;

    @Schema(description = ModelDescriptions.TaskResponse.UPDATED_BY)
    private String updatedBy;

    @Schema(description = ModelDescriptions.TaskResponse.DISABLED_AT)
    private Long disabledAt;

    @Schema(description = ModelDescriptions.TaskResponse.COMPLETED_AT)
    private Long completedAt;

    @Schema(description = ModelDescriptions.TaskResponse.VERSION)
    private Integer version;

    public MaintenanceWindowTaskResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public MaintenanceWindowTaskDependencyResponse getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(MaintenanceWindowTaskDependencyResponse dependsOn) {
        this.dependsOn = dependsOn;
    }

    public boolean isRetryWithinOccurrence() {
        return retryWithinOccurrence;
    }

    public void setRetryWithinOccurrence(boolean retryWithinOccurrence) {
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(Long disabledAt) {
        this.disabledAt = disabledAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowTaskResponse{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", taskType='" + taskType + '\'' +
                ", workItemId='" + workItemId + '\'' +
                ", taskKind='" + taskKind + '\'' +
                ", status='" + status + '\'' +
                ", submitterService='" + submitterService + '\'' +
                ", taskPayload=" + taskPayload +
                ", executionRef=" + executionRef +
                ", priority=" + priority +
                ", dependsOn=" + dependsOn +
                ", retryWithinOccurrence=" + retryWithinOccurrence +
                ", maxAttemptsPerOccurrence=" + maxAttemptsPerOccurrence +
                ", retryCooldownMinutes=" + retryCooldownMinutes +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", disabledAt=" + disabledAt +
                ", completedAt=" + completedAt +
                ", version=" + version +
                '}';
    }
}
