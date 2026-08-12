package com.sequenceiq.maintenance.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.maintenance.domain.converter.MaintenanceTaskKindConverter;
import com.sequenceiq.maintenance.domain.converter.MaintenanceTaskStatusConverter;

@Entity
@Table(name = "maintenance_window_task")
public class MaintenanceWindowTask implements Serializable {

    private static final int DEFAULT_PRIORITY = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "maintenance_window_task_generator")
    @SequenceGenerator(name = "maintenance_window_task_generator", sequenceName = "maintenance_window_task_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "resource_crn", nullable = false)
    private String resourceCrn;

    @Column(name = "environment_crn", nullable = false)
    private String environmentCrn;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "work_item_id", nullable = false)
    private String workItemId;

    @Convert(converter = MaintenanceTaskKindConverter.class)
    @Column(name = "task_kind", nullable = false)
    private MaintenanceTaskKind taskKind;

    @Convert(converter = MaintenanceTaskStatusConverter.class)
    @Column(nullable = false)
    private MaintenanceTaskStatus status;

    @Column(name = "submitter_service", nullable = false)
    private String submitterService;

    @Convert(converter = JsonToString.class)
    @Column(name = "task_payload", columnDefinition = "TEXT")
    private Json taskPayload;

    @Convert(converter = JsonToString.class)
    @Column(name = "execution_ref", columnDefinition = "TEXT", nullable = false)
    private Json executionRef;

    @Column(nullable = false)
    private Integer priority = DEFAULT_PRIORITY;

    @Column(name = "depends_on_task_id")
    private Long dependsOnTaskId;

    @Column(name = "retry_within_occurrence", nullable = false)
    private boolean retryWithinOccurrence;

    @Column(name = "max_attempts_per_occurrence", nullable = false)
    private Integer maxAttemptsPerOccurrence = 1;

    @Column(name = "retry_cooldown_minutes", nullable = false)
    private Integer retryCooldownMinutes = 0;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "disabled_at")
    private Long disabledAt;

    @Column(name = "completed_at")
    private Long completedAt;

    @Version
    @Column(nullable = false, insertable = false)
    private Integer version;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public MaintenanceTaskKind getTaskKind() {
        return taskKind;
    }

    public void setTaskKind(MaintenanceTaskKind taskKind) {
        this.taskKind = taskKind;
    }

    public MaintenanceTaskStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceTaskStatus status) {
        this.status = status;
    }

    public String getSubmitterService() {
        return submitterService;
    }

    public void setSubmitterService(String submitterService) {
        this.submitterService = submitterService;
    }

    public Json getTaskPayload() {
        return taskPayload;
    }

    public void setTaskPayload(Json taskPayload) {
        this.taskPayload = taskPayload;
    }

    public Json getExecutionRef() {
        return executionRef;
    }

    public void setExecutionRef(Json executionRef) {
        this.executionRef = executionRef;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public void setDependsOnTaskId(Long dependsOnTaskId) {
        this.dependsOnTaskId = dependsOnTaskId;
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
        return "MaintenanceWindowTask{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", taskType='" + taskType + '\'' +
                ", workItemId='" + workItemId + '\'' +
                ", taskKind=" + taskKind +
                ", status=" + status +
                ", submitterService='" + submitterService + '\'' +
                ", taskPayload=" + taskPayload +
                ", executionRef=" + executionRef +
                ", priority=" + priority +
                ", dependsOnTaskId=" + dependsOnTaskId +
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
