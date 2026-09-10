package com.sequenceiq.maintenance.dispatcher.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON body for the maintenance task execute callback posted to submitter services.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaintenanceTaskDispatchRequest(
        @JsonProperty("task_id") Long taskId,
        @JsonProperty("run_id") Long runId,
        @JsonProperty("idempotency_key") String idempotencyKey,
        @JsonProperty("account_id") String accountId,
        @JsonProperty("resource_crn") String resourceCrn,
        @JsonProperty("task_type") String taskType,
        @JsonProperty("work_item_id") String workItemId,
        @JsonProperty("task_kind") String taskKind,
        @JsonProperty("task_payload") Map<String, Object> taskPayload,
        @JsonProperty("maintenance_schedule_id") Long maintenanceScheduleId,
        @JsonProperty("policy_revision") String policyRevision,
        @JsonProperty("window_start") Long windowStart,
        @JsonProperty("window_end") Long windowEnd) {
}
