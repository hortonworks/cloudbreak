package com.sequenceiq.cloudbreak.api.v1.maintenance.model;

import java.util.Map;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.validation.AccountIdMatchesResourceCrn;
import com.sequenceiq.cloudbreak.validation.AccountIdMatchesResourceCrnGroup;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON body posted by the maintenance window dispatcher to submitter execute endpoints.
 * Keep field names and validation in sync with
 * {@code com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskDispatchRequest} in the maintenance module;
 * {@code MaintenanceTaskDispatchRequestContractTest} guards the wire format against drift.
 * <p>
 * This must stay a JavaBean (not a record): the authorization aspects resolve the {@link ResourceCrn} field via
 * {@code PropertyUtils.getProperty}, which needs a {@code getResourceCrn()} accessor to establish the target
 * tenant for {@code @InternalOnly} calls.
 */
@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@GroupSequence({MaintenanceTaskDispatchRequest.class, AccountIdMatchesResourceCrnGroup.class})
@AccountIdMatchesResourceCrn(groups = AccountIdMatchesResourceCrnGroup.class)
public class MaintenanceTaskDispatchRequest {

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

    @NotEmpty
    @ResourceCrn
    @ValidCrn(resource = CrnResourceDescriptor.DATAHUB)
    @JsonProperty("resource_crn")
    private String resourceCrn;

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

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
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
                ", resourceCrn='" + resourceCrn + '\'' +
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
