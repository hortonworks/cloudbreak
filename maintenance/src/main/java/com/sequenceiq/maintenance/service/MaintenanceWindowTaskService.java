package com.sequenceiq.maintenance.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskDependencyRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskListParams;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskListResponse;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;
import com.sequenceiq.maintenance.domain.MaintenanceEnumValues;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;

@Service
public class MaintenanceWindowTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowTaskService.class);

    private final MaintenanceWindowTaskRepository taskRepository;

    private final MaintenanceWindowTaskRegistrationTxService registrationTxService;

    private final MaintenanceWindowTaskValidator taskValidator;

    private final MaintenanceWindowTaskConverter taskConverter;

    private final Clock clock;

    @Inject
    public MaintenanceWindowTaskService(
            MaintenanceWindowTaskRepository taskRepository,
            MaintenanceWindowTaskRegistrationTxService registrationTxService,
            MaintenanceWindowTaskValidator taskValidator,
            MaintenanceWindowTaskConverter taskConverter,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.registrationTxService = registrationTxService;
        this.taskValidator = taskValidator;
        this.taskConverter = taskConverter;
        this.clock = clock;
    }

    @Transactional(TxType.REQUIRED)
    public TaskRegistrationResult register(MaintenanceWindowTaskRequest request, String accountId, String actor) {
        taskValidator.validateCreate(request, accountId);
        Optional<MaintenanceWindowTask> existingActive = taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                accountId, request.getResourceCrn(), request.getTaskType(), request.getWorkItemId());
        if (existingActive.isPresent()) {
            MaintenanceWindowTask existing = existingActive.get();
            taskValidator.validateIdempotentRegistration(existing, request, accountId);
            LOGGER.info("Returning existing ACTIVE maintenance task id={} taskType={} workItemId={}",
                    existing.getId(), existing.getTaskType(), existing.getWorkItemId());
            return new TaskRegistrationResult(toResponse(existing), false);
        }
        Long dependsOnTaskId = resolveDependsOnTaskId(accountId, request.getDependsOn());
        MaintenanceWindowTask task = taskConverter.toEntity(request);
        task.setAccountId(accountId);
        task.setStatus(MaintenanceTaskStatus.ACTIVE);
        task.setDependsOnTaskId(dependsOnTaskId);
        taskValidator.validateDependencyChain(
                accountId,
                task.getDependsOnTaskId(),
                request.getResourceCrn(),
                request.getTaskType(),
                request.getWorkItemId(),
                null);
        long now = clock.getCurrentTimeMillis();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setCreatedBy(actor);
        task.setUpdatedBy(actor);
        try {
            MaintenanceWindowTask saved = registrationTxService.save(task);
            LOGGER.info("Registered maintenance task id={} taskType={} workItemId={}",
                    saved.getId(), saved.getTaskType(), saved.getWorkItemId());
            return new TaskRegistrationResult(toResponse(saved), true);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            MaintenanceWindowTask raced = registrationTxService.findActive(
                    accountId, request.getResourceCrn(), request.getTaskType(), request.getWorkItemId())
                    .orElseThrow(() -> {
                        LOGGER.warn("Constraint violation during registration but no active task found, re-throwing", e);
                        return e;
                    });
            taskValidator.validateIdempotentRegistration(raced, request, accountId);
            LOGGER.info("Recovered concurrent registration for maintenance task id={} taskType={} workItemId={}",
                    raced.getId(), raced.getTaskType(), raced.getWorkItemId());
            return new TaskRegistrationResult(toResponse(raced), false);
        }
    }

    public MaintenanceWindowTaskResponse get(String accountId, Long taskId) {
        return toResponse(findRequired(accountId, taskId));
    }

    public MaintenanceWindowTaskListResponse list(String accountId, MaintenanceWindowTaskListParams params) {
        List<MaintenanceWindowTask> tasks = resolveListQuery(accountId, params);
        Map<Long, MaintenanceWindowTask> dependenciesById = loadDependencyTasks(accountId, tasks);
        MaintenanceWindowTaskListResponse response = new MaintenanceWindowTaskListResponse();
        response.setTasks(tasks.stream()
                .map(task -> toResponse(task, dependencyTask(task, dependenciesById)))
                .toList());
        return response;
    }

    private static MaintenanceWindowTask dependencyTask(
            MaintenanceWindowTask task, Map<Long, MaintenanceWindowTask> dependenciesById) {
        Long dependsOnTaskId = task.getDependsOnTaskId();
        return dependsOnTaskId == null ? null : dependenciesById.get(dependsOnTaskId);
    }

    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowTaskResponse update(
            String accountId, Long taskId, UpdateMaintenanceWindowTaskRequest request, String actor) {
        MaintenanceWindowTask task = findRequired(accountId, taskId);
        taskValidator.validateUpdate(task, request);
        Long dependsOnTaskId = request.getDependsOn() != null
                ? resolveDependsOnTaskId(accountId, request.getDependsOn())
                : task.getDependsOnTaskId();
        taskValidator.validateDependencyChain(
                accountId,
                dependsOnTaskId,
                task.getResourceCrn(),
                task.getTaskType(),
                task.getWorkItemId(),
                task.getId());
        MaintenanceTaskStatus previousStatus = task.getStatus();
        taskConverter.applyUpdateRequest(task, request);
        if (request.getDependsOn() != null) {
            task.setDependsOnTaskId(dependsOnTaskId);
        }
        if (task.getStatus() == MaintenanceTaskStatus.DISABLED && previousStatus != MaintenanceTaskStatus.DISABLED) {
            task.setDisabledAt(clock.getCurrentTimeMillis());
        }
        task.setUpdatedAt(clock.getCurrentTimeMillis());
        task.setUpdatedBy(actor);
        try {
            MaintenanceWindowTask saved = taskRepository.saveAndFlush(task);
            LOGGER.info("Updated maintenance task id={} status={}", saved.getId(), saved.getStatus());
            return toResponse(saved);
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("Task was modified concurrently.", e);
        }
    }

    @Transactional(TxType.REQUIRED)
    public void delete(String accountId, Long taskId, String actor) {
        MaintenanceWindowTask task = findRequired(accountId, taskId);
        if (task.getStatus() == MaintenanceTaskStatus.DELETED) {
            LOGGER.info("Delete skipped for already DELETED maintenance task id={}", taskId);
            return;
        }
        task.setStatus(MaintenanceTaskStatus.DELETED);
        task.setUpdatedAt(clock.getCurrentTimeMillis());
        task.setUpdatedBy(actor);
        try {
            taskRepository.saveAndFlush(task);
            LOGGER.info("Deleted maintenance task id={}", taskId);
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("Task was modified concurrently.", e);
        }
    }

    private Long resolveDependsOnTaskId(String accountId, MaintenanceWindowTaskDependencyRequest dependsOn) {
        if (dependsOn == null) {
            return null;
        }
        return taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                accountId, dependsOn.getResourceCrn(), dependsOn.getTaskType(), dependsOn.getWorkItemId())
                .map(MaintenanceWindowTask::getId)
                .orElseThrow(() -> new BadRequestException("dependsOn refers to an ACTIVE task that does not exist."));
    }

    private MaintenanceWindowTaskResponse toResponse(MaintenanceWindowTask task) {
        return toResponse(task, loadDependencyTask(task));
    }

    private MaintenanceWindowTaskResponse toResponse(MaintenanceWindowTask task, MaintenanceWindowTask dependencyTask) {
        return taskConverter.toResponse(task, dependencyTask);
    }

    private MaintenanceWindowTask loadDependencyTask(MaintenanceWindowTask task) {
        if (task.getDependsOnTaskId() == null) {
            return null;
        }
        return taskRepository.findByIdAndAccountId(task.getDependsOnTaskId(), task.getAccountId()).orElse(null);
    }

    private Map<Long, MaintenanceWindowTask> loadDependencyTasks(String accountId, List<MaintenanceWindowTask> tasks) {
        Set<Long> dependencyIds = tasks.stream()
                .map(MaintenanceWindowTask::getDependsOnTaskId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (dependencyIds.isEmpty()) {
            return Map.of();
        }
        return taskRepository.findByAccountIdAndIdIn(accountId, dependencyIds).stream()
                .collect(Collectors.toMap(MaintenanceWindowTask::getId, Function.identity()));
    }

    private List<MaintenanceWindowTask> resolveListQuery(String accountId, MaintenanceWindowTaskListParams params) {
        return taskRepository.findByAccountIdAndFilters(
                accountId,
                blankToNull(params.getResourceCrn()),
                blankToNull(params.getEnvironmentCrn()),
                blankToNull(params.getTaskType()),
                blankToNull(params.getWorkItemId()),
                MaintenanceEnumValues.toTaskKind(blankToNull(params.getTaskKind())),
                MaintenanceEnumValues.toTaskStatus(blankToNull(params.getStatus())));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private MaintenanceWindowTask findRequired(String accountId, Long taskId) {
        return taskRepository.findByIdAndAccountId(taskId, accountId)
                .orElseThrow(notFound(String.format("Maintenance task not found for accountId=%s taskId=%s", accountId, taskId)));
    }

    public record TaskRegistrationResult(MaintenanceWindowTaskResponse response, boolean created) {
    }
}
