package com.sequenceiq.maintenance.api.execution.model;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shared maintenance window dispatch payload fields for submitter execute endpoints (e.g. Cloudbreak, Data Lake).
 * Each service extends this type with a {@code resource_crn} field and {@code @ValidCrn} for its resource descriptor.
 * <p>
 * Keep field names in sync with {@code com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskDispatchRequest}
 * in the maintenance module. Subclasses must stay JavaBeans so authorization aspects can resolve {@code resource_crn}.
 * <p>
 * Do not move {@code resourceCrn} into this class: {@code InternalCrnModifier} finds the {@code @ResourceCrn} field via
 * {@code getClass().getDeclaredFields()}, which skips inherited fields. Declared here, the internal-actor tenant switch
 * silently stops working and every dispatch fails authorization. Each subclass must declare its own annotated field.
 */
@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class MaintenanceTaskDispatchRequestBase {

    @NotNull
    @JsonProperty("task_id")
    private Long taskId;

    @NotNull
    @JsonProperty("run_id")
    private Long runId;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @NotBlank
    @JsonProperty("account_id")
    private String accountId;

    @NotBlank
    @JsonProperty("task_type")
    private String taskType;

    @NotBlank
    @JsonProperty("work_item_id")
    private String workItemId;

    @NotBlank
    @JsonProperty("task_kind")
    private String taskKind;

    @JsonProperty("task_payload")
    private Map<String, Object> taskPayload;

    @JsonProperty("maintenance_schedule_id")
    private Long maintenanceScheduleId;

    @JsonProperty("policy_revision")
    private String policyRevision;

    @JsonProperty("window_start")
    private Long windowStart;

    @JsonProperty("window_end")
    private Long windowEnd;

    public abstract String getResourceCrn();

    public abstract void setResourceCrn(String resourceCrn);

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public Map<String, Object> getTaskPayload() {
        return taskPayload;
    }

    public void setTaskPayload(Map<String, Object> taskPayload) {
        this.taskPayload = taskPayload;
    }

    public Long getMaintenanceScheduleId() {
        return maintenanceScheduleId;
    }

    public void setMaintenanceScheduleId(Long maintenanceScheduleId) {
        this.maintenanceScheduleId = maintenanceScheduleId;
    }

    public String getPolicyRevision() {
        return policyRevision;
    }

    public void setPolicyRevision(String policyRevision) {
        this.policyRevision = policyRevision;
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

    @Override
    public String toString() {
        return "MaintenanceTaskDispatchRequest{" +
                "taskId=" + taskId +
                ", runId=" + runId +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + getResourceCrn() + '\'' +
                ", taskType='" + taskType + '\'' +
                ", workItemId='" + workItemId + '\'' +
                ", taskKind='" + taskKind + '\'' +
                ", maintenanceScheduleId=" + maintenanceScheduleId +
                ", policyRevision='" + policyRevision + '\'' +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                '}';
    }
}
