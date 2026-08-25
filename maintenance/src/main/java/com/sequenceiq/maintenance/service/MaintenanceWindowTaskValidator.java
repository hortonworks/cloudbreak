package com.sequenceiq.maintenance.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskDependencyRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.domain.MaintenanceEnumValues;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;
import com.sequenceiq.maintenance.util.MaintenanceTaskResourceScope;

@Component
public class MaintenanceWindowTaskValidator {

    public static final int MAX_ATTEMPTS_PER_OCCURRENCE = 10;

    public static final int MAX_DEPENDENCY_CHAIN_DEPTH = 10;

    private final MaintenanceTaskResourceScope taskResourceScope;

    private final MaintenanceWindowTaskRepository taskRepository;

    @Inject
    public MaintenanceWindowTaskValidator(
            MaintenanceTaskResourceScope taskResourceScope,
            MaintenanceWindowTaskRepository taskRepository) {
        this.taskResourceScope = taskResourceScope;
        this.taskRepository = taskRepository;
    }

    public void validateCreate(MaintenanceWindowTaskRequest request, String accountId) {
        taskResourceScope.validateTaskResourceCrn(request.getResourceCrn());
        validateCrnBelongsToAccount(request.getResourceCrn(), accountId, "resourceCrn");
        validateEnvironmentCrn(request.getEnvironmentCrn());
        validateCrnBelongsToAccount(request.getEnvironmentCrn(), accountId, "environmentCrn");
        validateRetryPolicy(request.getMaxAttemptsPerOccurrence(), request.getRetryCooldownMinutes());
        validateDependsOnReference(
                request.getDependsOn(), request.getResourceCrn(), request.getTaskType(), request.getWorkItemId(), accountId);
    }

    public void validateIdempotentRegistration(
            MaintenanceWindowTask existing, MaintenanceWindowTaskRequest request, String accountId) {
        requireIdempotentMatch("environmentCrn", Objects.equals(existing.getEnvironmentCrn(), request.getEnvironmentCrn()));
        requireIdempotentMatch("taskKind", existing.getTaskKind() == MaintenanceEnumValues.toTaskKind(request.getTaskKind()));
        requireIdempotentMatch("submitterService", Objects.equals(existing.getSubmitterService(), request.getSubmitterService()));
        requireIdempotentMatch("taskPayload", jsonMapsEqual(request.getTaskPayload(), existing.getTaskPayload()));
        requireIdempotentMatch("executionRef", jsonMapsEqual(request.getExecutionRef(), existing.getExecutionRef()));
        requireIdempotentMatch("dependsOn", dependsOnReferencesMatch(existing, request.getDependsOn(), accountId));
        if (request.getPriority() != null) {
            requireIdempotentMatch("priority", Objects.equals(existing.getPriority(), request.getPriority()));
        }
        if (request.getRetryWithinOccurrence() != null) {
            requireIdempotentMatch("retryWithinOccurrence",
                    request.getRetryWithinOccurrence().equals(existing.isRetryWithinOccurrence()));
        }
        if (request.getMaxAttemptsPerOccurrence() != null) {
            requireIdempotentMatch("maxAttemptsPerOccurrence",
                    Objects.equals(existing.getMaxAttemptsPerOccurrence(), request.getMaxAttemptsPerOccurrence()));
        }
        if (request.getRetryCooldownMinutes() != null) {
            requireIdempotentMatch("retryCooldownMinutes",
                    Objects.equals(existing.getRetryCooldownMinutes(), request.getRetryCooldownMinutes()));
        }
    }

    public void validateUpdate(MaintenanceWindowTask task, UpdateMaintenanceWindowTaskRequest request) {
        if (request.getStatus() != null && MaintenanceTaskStatus.DISABLED != MaintenanceEnumValues.toTaskStatus(request.getStatus())) {
            throw new BadRequestException("Only DISABLED is supported for status updates.");
        }
        if (task.getStatus() == MaintenanceTaskStatus.DELETED) {
            throw new BadRequestException("Deleted tasks cannot be updated.");
        }
        if (task.getStatus() == MaintenanceTaskStatus.COMPLETED) {
            throw new BadRequestException("Completed tasks cannot be updated.");
        }
        Integer maxAttempts = request.getMaxAttemptsPerOccurrence() != null
                ? request.getMaxAttemptsPerOccurrence()
                : task.getMaxAttemptsPerOccurrence();
        Integer retryCooldown = request.getRetryCooldownMinutes() != null
                ? request.getRetryCooldownMinutes()
                : task.getRetryCooldownMinutes();
        validateRetryPolicy(maxAttempts, retryCooldown);
        if (request.getDependsOn() != null) {
            validateDependsOnReference(
                    request.getDependsOn(), task.getResourceCrn(), task.getTaskType(), task.getWorkItemId(), task.getAccountId());
        }
    }

    public void validateDependencyChain(
            String accountId,
            Long dependsOnTaskId,
            String resourceCrn,
            String taskType,
            String workItemId,
            Long excludeTaskId) {
        if (dependsOnTaskId == null) {
            return;
        }
        Set<Long> visited = new HashSet<>();
        Long currentTaskId = dependsOnTaskId;
        while (currentTaskId != null) {
            currentTaskId = nextDependencyTaskId(
                    accountId, currentTaskId, visited, resourceCrn, taskType, workItemId, excludeTaskId);
        }
    }

    private Long nextDependencyTaskId(
            String accountId,
            Long currentTaskId,
            Set<Long> visited,
            String resourceCrn,
            String taskType,
            String workItemId,
            Long excludeTaskId) {
        if (visited.size() >= MAX_DEPENDENCY_CHAIN_DEPTH) {
            throw new BadRequestException(
                    "dependsOn chain exceeds the maximum depth of " + MAX_DEPENDENCY_CHAIN_DEPTH + ".");
        }
        if (!visited.add(currentTaskId)) {
            throw new BadRequestException("dependsOn chain contains a cycle.");
        }
        if (excludeTaskId != null && excludeTaskId.equals(currentTaskId)) {
            throw new BadRequestException("dependsOn would create a dependency cycle.");
        }
        MaintenanceWindowTask current = taskRepository.findByIdAndAccountId(currentTaskId, accountId)
                .orElseThrow(() -> new BadRequestException("dependsOn refers to a task that does not exist."));
        if (current.getStatus() != MaintenanceTaskStatus.ACTIVE) {
            throw new BadRequestException("dependsOn chain includes a non-ACTIVE task.");
        }
        if (sameWorkItem(current, resourceCrn, taskType, workItemId)) {
            throw new BadRequestException("dependsOn would create a dependency cycle.");
        }
        return current.getDependsOnTaskId();
    }

    private static boolean sameWorkItem(
            MaintenanceWindowTask task, String resourceCrn, String taskType, String workItemId) {
        return resourceCrn.equals(task.getResourceCrn())
                && taskType.equals(task.getTaskType())
                && workItemId.equals(task.getWorkItemId());
    }

    private void validateEnvironmentCrn(String environmentCrn) {
        if (StringUtils.isBlank(environmentCrn) || !Crn.isCrn(environmentCrn)) {
            throw new BadRequestException("environmentCrn must be a valid CRN.");
        }
        Crn crn = Crn.fromString(environmentCrn);
        if (crn.getResourceType() != Crn.ResourceType.ENVIRONMENT) {
            throw new BadRequestException("environmentCrn must reference an environment resource.");
        }
    }

    private void validateRetryPolicy(Integer maxAttemptsPerOccurrence, Integer retryCooldownMinutes) {
        if (maxAttemptsPerOccurrence != null && maxAttemptsPerOccurrence > MAX_ATTEMPTS_PER_OCCURRENCE) {
            throw new BadRequestException(
                    "maxAttemptsPerOccurrence must be at most " + MAX_ATTEMPTS_PER_OCCURRENCE + ".");
        }
        if (retryCooldownMinutes != null && retryCooldownMinutes < 0) {
            throw new BadRequestException("retryCooldownMinutes must be non-negative.");
        }
    }

    private void validateDependsOnReference(
            MaintenanceWindowTaskDependencyRequest dependsOn,
            String resourceCrn,
            String taskType,
            String workItemId,
            String accountId) {
        if (dependsOn == null) {
            return;
        }
        taskResourceScope.validateTaskResourceCrn(dependsOn.getResourceCrn());
        validateCrnBelongsToAccount(dependsOn.getResourceCrn(), accountId, "dependsOn.resourceCrn");
        if (sameWorkItem(dependsOn.getResourceCrn(), dependsOn.getTaskType(), dependsOn.getWorkItemId(), resourceCrn, taskType, workItemId)) {
            throw new BadRequestException("dependsOn cannot reference the same task.");
        }
    }

    private static boolean sameWorkItem(
            String resourceCrn, String taskType, String workItemId,
            String otherResourceCrn, String otherTaskType, String otherWorkItemId) {
        return resourceCrn.equals(otherResourceCrn)
                && taskType.equals(otherTaskType)
                && workItemId.equals(otherWorkItemId);
    }

    private void validateCrnBelongsToAccount(String crn, String accountId, String fieldName) {
        if (!accountId.equals(Crn.fromString(crn).getAccountId())) {
            throw new BadRequestException(fieldName + " must belong to the same account as accountId.");
        }
    }

    private boolean dependsOnReferencesMatch(
            MaintenanceWindowTask existing, MaintenanceWindowTaskDependencyRequest requestDependsOn, String accountId) {
        Long existingDependsOnTaskId = existing.getDependsOnTaskId();
        if (requestDependsOn == null) {
            return existingDependsOnTaskId == null;
        }
        if (existingDependsOnTaskId == null) {
            return false;
        }
        return taskRepository.findByIdAndAccountId(existingDependsOnTaskId, accountId)
                .map(dependency -> sameWorkItem(
                        dependency,
                        requestDependsOn.getResourceCrn(),
                        requestDependsOn.getTaskType(),
                        requestDependsOn.getWorkItemId()))
                .orElse(false);
    }

    private static void requireIdempotentMatch(String fieldName, boolean matches) {
        if (!matches) {
            throw new ConflictException(
                    "An ACTIVE task already exists for this work item with a different " + fieldName + ".");
        }
    }

    private static boolean jsonMapsEqual(Map<String, Object> requestMap, Json entityJson) {
        if (requestMap == null) {
            return entityJson == null;
        }
        if (entityJson == null) {
            return false;
        }
        try {
            return entityJson.equals(new Json(requestMap));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
