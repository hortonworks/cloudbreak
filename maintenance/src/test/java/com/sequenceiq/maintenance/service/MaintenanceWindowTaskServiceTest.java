package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskDependencyRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskListParams;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskListResponse;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;
import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;
import com.sequenceiq.maintenance.service.MaintenanceWindowTaskService.TaskRegistrationResult;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowTaskServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String ACTOR = "crn:altus:iam:us-west-1:acc-1:user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";

    private static final long NOW = 1_735_689_600_000L;

    @Mock
    private MaintenanceWindowTaskRepository taskRepository;

    @Mock
    private MaintenanceWindowTaskRegistrationTxService registrationTxService;

    @Mock
    private Clock clock;

    private MaintenanceWindowTaskValidator taskValidator;

    private MaintenanceWindowTaskConverter taskConverter;

    private MaintenanceWindowTaskService taskService;

    @BeforeEach
    void setUp() {
        taskValidator = new MaintenanceWindowTaskValidator(new com.sequenceiq.maintenance.util.MaintenanceTaskResourceScope(), taskRepository);
        taskConverter = new MaintenanceWindowTaskConverter();
        taskService = new MaintenanceWindowTaskService(
                taskRepository, registrationTxService, taskValidator, taskConverter, clock);
    }

    @Test
    void registerCreatesActiveTask() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowTaskRequest request = createRequest();
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.empty());
        MaintenanceWindowTask saved = entityFromRequest(request);
        saved.setId(42L);
        when(registrationTxService.save(any(MaintenanceWindowTask.class))).thenReturn(saved);

        TaskRegistrationResult result = taskService.register(request, ACCOUNT_ID, ACTOR);

        assertThat(result.created()).isTrue();
        assertThat(result.response().getId()).isEqualTo(42L);
        assertThat(result.response().getStatus()).isEqualTo("ACTIVE");
        assertThat(result.response().getTaskKind()).isEqualTo("EVERY_WINDOW");
    }

    @Test
    void registerReturnsExistingActiveTaskWhenPayloadMatches() {
        MaintenanceWindowTaskRequest request = createRequest();
        MaintenanceWindowTask existing = entityFromRequest(request);
        existing.setId(7L);
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.of(existing));

        TaskRegistrationResult result = taskService.register(request, ACCOUNT_ID, ACTOR);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getId()).isEqualTo(7L);
        verify(registrationTxService, never()).save(any(MaintenanceWindowTask.class));
    }

    @Test
    void registerRejectsConflictingExistingActiveTask() {
        MaintenanceWindowTaskRequest request = createRequest();
        MaintenanceWindowTask existing = entityFromRequest(request);
        existing.setId(7L);
        existing.setExecutionRef(new com.sequenceiq.cloudbreak.common.json.Json(Map.of("type", "http", "url", "http://example/other")));
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.of(existing));

        assertThrows(com.sequenceiq.maintenance.exception.ConflictException.class,
                () -> taskService.register(request, ACCOUNT_ID, ACTOR));
        verify(registrationTxService, never()).save(any(MaintenanceWindowTask.class));
    }

    @Test
    void disableRemovesTaskFromActiveSet() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowTask task = entityFromRequest(createRequest());
        task.setId(5L);
        task.setAccountId(ACCOUNT_ID);
        when(taskRepository.findByIdAndAccountId(5L, ACCOUNT_ID)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);

        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setStatus("DISABLED");

        MaintenanceWindowTaskResponse response = taskService.update(ACCOUNT_ID, 5L, request, ACTOR);

        assertThat(response.getStatus()).isEqualTo("DISABLED");
        assertThat(task.getDisabledAt()).isEqualTo(NOW);
    }

    @Test
    void updateThrowsConflictOnOptimisticLockFailureWithoutDependency() {
        MaintenanceWindowTask task = entityFromRequest(createRequest());
        task.setId(5L);
        task.setAccountId(ACCOUNT_ID);
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        when(taskRepository.findByIdAndAccountId(5L, ACCOUNT_ID)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenThrow(new ObjectOptimisticLockingFailureException(MaintenanceWindowTask.class, 5L));

        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setStatus("DISABLED");

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.update(ACCOUNT_ID, 5L, request, ACTOR));

        assertThat(exception.getMessage()).isEqualTo("Task was modified concurrently.");
        verify(taskRepository).saveAndFlush(task);
        verify(taskRepository, times(1)).findByIdAndAccountId(5L, ACCOUNT_ID);
    }

    @Test
    void deleteThrowsConflictOnOptimisticLockFailure() {
        MaintenanceWindowTask task = entityFromRequest(createRequest());
        task.setId(9L);
        task.setAccountId(ACCOUNT_ID);
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        when(taskRepository.findByIdAndAccountId(9L, ACCOUNT_ID)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenThrow(new ObjectOptimisticLockingFailureException(MaintenanceWindowTask.class, 9L));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> taskService.delete(ACCOUNT_ID, 9L, ACTOR));

        assertThat(exception.getMessage()).isEqualTo("Task was modified concurrently.");
    }

    @Test
    void deleteSoftDeletesTask() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowTask task = entityFromRequest(createRequest());
        task.setId(9L);
        task.setAccountId(ACCOUNT_ID);
        when(taskRepository.findByIdAndAccountId(9L, ACCOUNT_ID)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);

        taskService.delete(ACCOUNT_ID, 9L, ACTOR);

        assertThat(task.getStatus()).isEqualTo(MaintenanceTaskStatus.DELETED);
    }

    @Test
    void listFiltersByResourceCrnAndStatus() {
        MaintenanceWindowTask task = entityFromRequest(createRequest());
        task.setId(1L);
        task.setAccountId(ACCOUNT_ID);
        when(taskRepository.findByAccountIdAndFilters(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(task));

        MaintenanceWindowTaskListParams params = new MaintenanceWindowTaskListParams();
        params.setResourceCrn(DATAHUB_CRN);
        params.setStatus("ACTIVE");

        assertThat(taskService.list(ACCOUNT_ID, params).getTasks()).hasSize(1);
        verify(taskRepository, never()).findByIdAndAccountId(any(), eq(ACCOUNT_ID));
    }

    @Test
    void listBatchLoadsDependencies() {
        MaintenanceWindowTask dependency = entityFromRequest(createRequest());
        dependency.setId(10L);
        dependency.setTaskType("DATABASE_UPGRADE");
        dependency.setWorkItemId("external-postgres:15");

        MaintenanceWindowTask dependent = entityFromRequest(createRequest());
        dependent.setId(11L);
        dependent.setTaskType("RUNTIME_UPGRADE");
        dependent.setWorkItemId("runtime:7.2.18");
        dependent.setDependsOnTaskId(10L);
        dependent.setAccountId(ACCOUNT_ID);

        when(taskRepository.findByAccountIdAndFilters(
                eq(ACCOUNT_ID), isNull(String.class), isNull(String.class), isNull(String.class), isNull(String.class),
                isNull(MaintenanceTaskKind.class), isNull(MaintenanceTaskStatus.class)))
                .thenReturn(List.of(dependent));
        when(taskRepository.findByAccountIdAndIdIn(ACCOUNT_ID, Set.of(10L))).thenReturn(List.of(dependency));

        MaintenanceWindowTaskListResponse response = taskService.list(ACCOUNT_ID, new MaintenanceWindowTaskListParams());

        assertThat(response.getTasks()).hasSize(1);
        assertThat(response.getTasks().get(0).getDependsOn().getTaskType()).isEqualTo("DATABASE_UPGRADE");
        verify(taskRepository).findByAccountIdAndIdIn(ACCOUNT_ID, Set.of(10L));
        verify(taskRepository, never()).findByIdAndAccountId(any(), eq(ACCOUNT_ID));
    }

    @Test
    void registerResolvesDependsOnToActiveTaskId() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowTaskRequest dependencyRequest = createRequest();
        dependencyRequest.setTaskType("DATABASE_UPGRADE");
        dependencyRequest.setWorkItemId("external-postgres:15");
        MaintenanceWindowTask dependency = entityFromRequest(dependencyRequest);
        dependency.setId(10L);

        MaintenanceWindowTaskRequest request = createRequest();
        request.setTaskType("RUNTIME_UPGRADE");
        request.setWorkItemId("runtime:7.2.18");
        request.setDependsOn(dependencyReference(DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15"));

        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "RUNTIME_UPGRADE", "runtime:7.2.18")).thenReturn(Optional.empty());
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15")).thenReturn(Optional.of(dependency));
        when(registrationTxService.save(any(MaintenanceWindowTask.class))).thenAnswer(invocation -> {
            MaintenanceWindowTask task = invocation.getArgument(0);
            task.setId(11L);
            return task;
        });
        when(taskRepository.findByIdAndAccountId(10L, ACCOUNT_ID)).thenReturn(Optional.of(dependency));

        TaskRegistrationResult result = taskService.register(request, ACCOUNT_ID, ACTOR);

        assertThat(result.response().getDependsOn().getTaskType()).isEqualTo("DATABASE_UPGRADE");
        assertThat(result.response().getDependsOn().getWorkItemId()).isEqualTo("external-postgres:15");
    }

    @Test
    void registerReturnsExistingTaskAfterUniqueConstraintViolation() {
        MaintenanceWindowTaskRequest request = createRequest();
        MaintenanceWindowTask existing = entityFromRequest(request);
        existing.setId(99L);
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.empty());
        when(registrationTxService.save(any(MaintenanceWindowTask.class)))
                .thenThrow(new DuplicateKeyException("unique violation"));
        when(registrationTxService.findActive(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.of(existing));

        TaskRegistrationResult result = taskService.register(request, ACCOUNT_ID, ACTOR);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getId()).isEqualTo(99L);
        verify(registrationTxService).findActive(ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1");
    }

    @Test
    void registerRejectsConflictingTaskAfterUniqueConstraintViolation() {
        MaintenanceWindowTaskRequest request = createRequest();
        MaintenanceWindowTask existing = entityFromRequest(request);
        existing.setId(99L);
        existing.setExecutionRef(new com.sequenceiq.cloudbreak.common.json.Json(
                Map.of("type", "http", "url", "http://example/other")));
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.empty());
        when(registrationTxService.save(any(MaintenanceWindowTask.class)))
                .thenThrow(new DuplicateKeyException("unique violation"));
        when(registrationTxService.findActive(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.of(existing));

        assertThrows(com.sequenceiq.maintenance.exception.ConflictException.class,
                () -> taskService.register(request, ACCOUNT_ID, ACTOR));
    }

    @Test
    void registerRethrowsWhenUniqueConstraintViolationHasNoMatchingActiveTask() {
        MaintenanceWindowTaskRequest request = createRequest();
        DataIntegrityViolationException failure = new DuplicateKeyException("unique violation");
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.empty());
        when(registrationTxService.save(any(MaintenanceWindowTask.class))).thenThrow(failure);
        when(registrationTxService.findActive(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.empty());

        assertThrows(DataIntegrityViolationException.class, () -> taskService.register(request, ACCOUNT_ID, ACTOR));
    }

    @Test
    void registerReturnsExistingTaskWhenDependencyNoLongerActive() {
        MaintenanceWindowTaskRequest dependencyRequest = createRequest();
        dependencyRequest.setTaskType("DATABASE_UPGRADE");
        dependencyRequest.setWorkItemId("external-postgres:15");
        MaintenanceWindowTask dependency = entityFromRequest(dependencyRequest);
        dependency.setId(10L);
        dependency.setStatus(MaintenanceTaskStatus.COMPLETED);

        MaintenanceWindowTaskRequest request = createRequest();
        request.setTaskType("RUNTIME_UPGRADE");
        request.setWorkItemId("runtime:7.2.18");
        request.setDependsOn(dependencyReference(DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15"));

        MaintenanceWindowTask existing = entityFromRequest(request);
        existing.setId(11L);
        existing.setDependsOnTaskId(10L);

        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "RUNTIME_UPGRADE", "runtime:7.2.18")).thenReturn(Optional.of(existing));
        when(taskRepository.findByIdAndAccountId(10L, ACCOUNT_ID)).thenReturn(Optional.of(dependency));

        TaskRegistrationResult result = taskService.register(request, ACCOUNT_ID, ACTOR);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getId()).isEqualTo(11L);
        verify(taskRepository, never()).findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15");
        verify(registrationTxService, never()).save(any(MaintenanceWindowTask.class));
    }

    @Test
    void registerRejectsMissingDependsOnTask() {
        MaintenanceWindowTaskRequest request = createRequest();
        request.setDependsOn(dependencyReference(DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15"));
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "secret-rotation", "secret-1")).thenReturn(Optional.empty());
        when(taskRepository.findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
                ACCOUNT_ID, DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> taskService.register(request, ACCOUNT_ID, ACTOR));
    }

    private MaintenanceWindowTaskDependencyRequest dependencyReference(String resourceCrn, String taskType, String workItemId) {
        MaintenanceWindowTaskDependencyRequest dependsOn = new MaintenanceWindowTaskDependencyRequest();
        dependsOn.setResourceCrn(resourceCrn);
        dependsOn.setTaskType(taskType);
        dependsOn.setWorkItemId(workItemId);
        return dependsOn;
    }

    private MaintenanceWindowTaskRequest createRequest() {
        MaintenanceWindowTaskRequest request = new MaintenanceWindowTaskRequest();
        request.setResourceCrn(DATAHUB_CRN);
        request.setEnvironmentCrn(ENV_CRN);
        request.setTaskType("secret-rotation");
        request.setWorkItemId("secret-1");
        request.setTaskKind("EVERY_WINDOW");
        request.setSubmitterService("secret-rotation-service");
        request.setExecutionRef(Map.of("type", "http", "url", "http://example/execute"));
        request.setTaskPayload(Map.of("secretId", "secret-1"));
        request.setPriority(200);
        request.setRetryWithinOccurrence(true);
        request.setMaxAttemptsPerOccurrence(3);
        request.setRetryCooldownMinutes(5);
        return request;
    }

    private MaintenanceWindowTask entityFromRequest(MaintenanceWindowTaskRequest request) {
        MaintenanceWindowTask task = new MaintenanceWindowTaskConverter().toEntity(request);
        task.setAccountId(ACCOUNT_ID);
        task.setStatus(MaintenanceTaskStatus.ACTIVE);
        task.setCreatedAt(NOW);
        task.setUpdatedAt(NOW);
        task.setCreatedBy(ACTOR);
        task.setUpdatedBy(ACTOR);
        task.setTaskKind(MaintenanceTaskKind.EVERY_WINDOW);
        task.setVersion(1);
        return task;
    }
}
