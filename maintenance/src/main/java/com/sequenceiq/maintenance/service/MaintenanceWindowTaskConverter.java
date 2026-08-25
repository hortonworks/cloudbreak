package com.sequenceiq.maintenance.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskDependencyResponse;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;
import com.sequenceiq.maintenance.domain.MaintenanceEnumValues;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;

@Component
public class MaintenanceWindowTaskConverter {

    public MaintenanceWindowTask toEntity(MaintenanceWindowTaskRequest request) {
        MaintenanceWindowTask task = new MaintenanceWindowTask();
        task.setResourceCrn(request.getResourceCrn());
        task.setEnvironmentCrn(request.getEnvironmentCrn());
        task.setTaskType(request.getTaskType());
        task.setWorkItemId(request.getWorkItemId());
        task.setTaskKind(MaintenanceEnumValues.toTaskKind(request.getTaskKind()));
        task.setSubmitterService(request.getSubmitterService());
        task.setTaskPayload(toOptionalJson(request.getTaskPayload(), "taskPayload"));
        task.setExecutionRef(toRequiredJson(request.getExecutionRef(), "executionRef"));
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getRetryWithinOccurrence() != null) {
            task.setRetryWithinOccurrence(request.getRetryWithinOccurrence());
        }
        if (request.getMaxAttemptsPerOccurrence() != null) {
            task.setMaxAttemptsPerOccurrence(request.getMaxAttemptsPerOccurrence());
        }
        if (request.getRetryCooldownMinutes() != null) {
            task.setRetryCooldownMinutes(request.getRetryCooldownMinutes());
        }
        return task;
    }

    public void applyUpdateRequest(MaintenanceWindowTask task, UpdateMaintenanceWindowTaskRequest request) {
        if (request.getStatus() != null) {
            task.setStatus(MaintenanceEnumValues.toTaskStatus(request.getStatus()));
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getRetryWithinOccurrence() != null) {
            task.setRetryWithinOccurrence(request.getRetryWithinOccurrence());
        }
        if (request.getMaxAttemptsPerOccurrence() != null) {
            task.setMaxAttemptsPerOccurrence(request.getMaxAttemptsPerOccurrence());
        }
        if (request.getRetryCooldownMinutes() != null) {
            task.setRetryCooldownMinutes(request.getRetryCooldownMinutes());
        }
        if (request.getTaskPayload() != null) {
            task.setTaskPayload(toRequiredJson(request.getTaskPayload(), "taskPayload"));
        }
        if (request.getExecutionRef() != null) {
            task.setExecutionRef(toRequiredJson(request.getExecutionRef(), "executionRef"));
        }
    }

    /**
     * Maps a persisted task entity to an API response.
     */
    public MaintenanceWindowTaskResponse toResponse(MaintenanceWindowTask task, MaintenanceWindowTask dependencyTask) {
        MaintenanceWindowTaskResponse response = new MaintenanceWindowTaskResponse();
        response.setId(task.getId());
        response.setAccountId(task.getAccountId());
        response.setResourceCrn(task.getResourceCrn());
        response.setEnvironmentCrn(task.getEnvironmentCrn());
        response.setTaskType(task.getTaskType());
        response.setWorkItemId(task.getWorkItemId());
        response.setTaskKind(task.getTaskKind() == null ? null : task.getTaskKind().name());
        response.setStatus(task.getStatus() == null ? null : task.getStatus().name());
        response.setSubmitterService(task.getSubmitterService());
        response.setTaskPayload(toMapOrNull(task.getTaskPayload()));
        response.setExecutionRef(toMapOrNull(task.getExecutionRef()));
        response.setPriority(task.getPriority());
        if (dependencyTask != null) {
            response.setDependsOn(toDependencyResponse(dependencyTask));
        }
        response.setRetryWithinOccurrence(task.isRetryWithinOccurrence());
        response.setMaxAttemptsPerOccurrence(task.getMaxAttemptsPerOccurrence());
        response.setRetryCooldownMinutes(task.getRetryCooldownMinutes());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setCreatedBy(task.getCreatedBy());
        response.setUpdatedBy(task.getUpdatedBy());
        response.setDisabledAt(task.getDisabledAt());
        response.setCompletedAt(task.getCompletedAt());
        response.setVersion(task.getVersion());
        return response;
    }

    public MaintenanceWindowTaskDependencyResponse toDependencyResponse(MaintenanceWindowTask task) {
        return new MaintenanceWindowTaskDependencyResponse(task.getResourceCrn(), task.getTaskType(), task.getWorkItemId());
    }

    private static Map<String, Object> toMapOrNull(Json json) {
        return json == null ? null : json.getMap();
    }

    private static Json toOptionalJson(Map<String, Object> value, String fieldName) {
        return value == null ? null : toRequiredJson(value, fieldName);
    }

    private static Json toRequiredJson(Map<String, Object> value, String fieldName) {
        try {
            return new Json(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(fieldName + " must be JSON-serializable.", e);
        }
    }
}
