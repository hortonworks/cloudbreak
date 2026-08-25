package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskDependencyRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;
import com.sequenceiq.maintenance.util.MaintenanceTaskResourceScope;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowTaskValidatorTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";

    @Mock
    private MaintenanceWindowTaskRepository taskRepository;

    private MaintenanceWindowTaskValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new MaintenanceWindowTaskValidator(new MaintenanceTaskResourceScope(), taskRepository);
    }

    @Test
    void rejectsMaxAttemptsAbovePlatformLimit() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setMaxAttemptsPerOccurrence(MaintenanceWindowTaskValidator.MAX_ATTEMPTS_PER_OCCURRENCE + 1);

        assertThatThrownBy(() -> underTest.validateCreate(request, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsSelfDependencyOnCreate() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setDependsOn(dependencyRequest(DATAHUB_CRN, "secret-rotation", "secret-1"));

        assertThatThrownBy(() -> underTest.validateCreate(request, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("same task");
    }

    @Test
    void rejectsUpdateOfCompletedTask() {
        MaintenanceWindowTask task = activeTask(5L, "secret-rotation", "secret-1", null);
        task.setStatus(MaintenanceTaskStatus.COMPLETED);
        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setPriority(150);

        assertThatThrownBy(() -> underTest.validateUpdate(task, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Completed tasks cannot be updated");
    }

    @Test
    void rejectsSelfDependencyOnUpdate() {
        MaintenanceWindowTask task = activeTask(5L, "secret-rotation", "secret-1", null);
        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setDependsOn(dependencyRequest(DATAHUB_CRN, "secret-rotation", "secret-1"));

        assertThatThrownBy(() -> underTest.validateUpdate(task, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("same task");
    }

    @Test
    void rejectsResourceCrnFromDifferentAccount() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setResourceCrn("crn:cdp:datahub:us-west-1:other-account:cluster:dh-1");

        assertThatThrownBy(() -> underTest.validateCreate(request, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("resourceCrn must belong to the same account");
    }

    @Test
    void rejectsEnvironmentCrnFromDifferentAccount() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setEnvironmentCrn("crn:cdp:environments:us-west-1:other-account:environment:env-1");

        assertThatThrownBy(() -> underTest.validateCreate(request, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("environmentCrn must belong to the same account");
    }

    @Test
    void rejectsDependsOnResourceCrnFromDifferentAccount() {
        MaintenanceWindowTaskRequest request = validRequest();
        request.setDependsOn(dependencyRequest(
                "crn:cdp:datahub:us-west-1:other-account:cluster:dh-1", "DATABASE_UPGRADE", "external-postgres:15"));

        assertThatThrownBy(() -> underTest.validateCreate(request, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dependsOn.resourceCrn must belong to the same account");
    }

    @Test
    void rejectsDependencyChainThroughNonActiveTask() {
        MaintenanceWindowTask disabled = activeTask(2L, "RUNTIME_UPGRADE", "runtime:7.2.18", null);
        disabled.setStatus(MaintenanceTaskStatus.DISABLED);
        when(taskRepository.findByIdAndAccountId(2L, ACCOUNT_ID)).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> underTest.validateDependencyChain(
                ACCOUNT_ID, 2L, DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("non-ACTIVE task");
    }

    @Test
    void rejectsDependencyChainExceedingMaxDepth() {
        for (long id = 2; id <= MaintenanceWindowTaskValidator.MAX_DEPENDENCY_CHAIN_DEPTH + 1; id++) {
            MaintenanceWindowTask task = activeTask(id, "task-" + id, "item-" + id, id - 1);
            when(taskRepository.findByIdAndAccountId(id, ACCOUNT_ID)).thenReturn(Optional.of(task));
        }

        assertThatThrownBy(() -> underTest.validateDependencyChain(
                ACCOUNT_ID,
                MaintenanceWindowTaskValidator.MAX_DEPENDENCY_CHAIN_DEPTH + 1L,
                DATAHUB_CRN,
                "DATABASE_UPGRADE",
                "external-postgres:15",
                1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maximum depth");
    }

    @Test
    void rejectsIdempotentRegistrationPayloadMismatch() {
        MaintenanceWindowTask existing = activeTask(5L, "secret-rotation", "secret-1", null);
        existing.setEnvironmentCrn(ENV_CRN);
        existing.setTaskKind(MaintenanceTaskKind.ONE_SHOT);
        existing.setSubmitterService("secret-rotation-service");
        existing.setExecutionRef(new com.sequenceiq.cloudbreak.common.json.Json(Map.of("type", "http", "url", "http://example/a")));
        MaintenanceWindowTaskRequest request = validRequest();
        request.setExecutionRef(Map.of("type", "http", "url", "http://example/b"));

        assertThatThrownBy(() -> underTest.validateIdempotentRegistration(existing, request, ACCOUNT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("executionRef");
    }

    @Test
    void acceptsIdempotentRegistrationWhenDependencyIsNoLongerActive() {
        MaintenanceWindowTask dependency = activeTask(10L, "DATABASE_UPGRADE", "external-postgres:15", null);
        dependency.setStatus(MaintenanceTaskStatus.COMPLETED);
        MaintenanceWindowTask existing = activeTask(11L, "RUNTIME_UPGRADE", "runtime:7.2.18", 10L);
        existing.setEnvironmentCrn(ENV_CRN);
        existing.setTaskKind(MaintenanceTaskKind.ONE_SHOT);
        existing.setSubmitterService("secret-rotation-service");
        existing.setExecutionRef(new com.sequenceiq.cloudbreak.common.json.Json(Map.of("type", "http")));
        when(taskRepository.findByIdAndAccountId(10L, ACCOUNT_ID)).thenReturn(Optional.of(dependency));

        MaintenanceWindowTaskRequest request = validRequest();
        request.setTaskType("RUNTIME_UPGRADE");
        request.setWorkItemId("runtime:7.2.18");
        request.setDependsOn(dependencyRequest(DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15"));

        underTest.validateIdempotentRegistration(existing, request, ACCOUNT_ID);
    }

    @Test
    void rejectsTransitiveDependencyCycle() {
        MaintenanceWindowTask taskB = activeTask(2L, "RUNTIME_UPGRADE", "runtime:7.2.18", 1L);
        when(taskRepository.findByIdAndAccountId(2L, ACCOUNT_ID)).thenReturn(Optional.of(taskB));

        assertThatThrownBy(() -> underTest.validateDependencyChain(
                ACCOUNT_ID, 2L, DATAHUB_CRN, "DATABASE_UPGRADE", "external-postgres:15", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dependency cycle");
    }

    @Test
    void rejectsDependencyCycleByIdentityTupleOnCreate() {
        MaintenanceWindowTask dependency = activeTask(9L, "RUNTIME_UPGRADE", "runtime:7.2.18", null);
        when(taskRepository.findByIdAndAccountId(9L, ACCOUNT_ID)).thenReturn(Optional.of(dependency));

        assertThatThrownBy(() -> underTest.validateDependencyChain(
                ACCOUNT_ID, 9L, DATAHUB_CRN, "RUNTIME_UPGRADE", "runtime:7.2.18", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dependency cycle");
    }

    private MaintenanceWindowTaskRequest validRequest() {
        MaintenanceWindowTaskRequest request = new MaintenanceWindowTaskRequest();
        request.setResourceCrn(DATAHUB_CRN);
        request.setEnvironmentCrn(ENV_CRN);
        request.setTaskType("secret-rotation");
        request.setWorkItemId("secret-1");
        request.setTaskKind("ONE_SHOT");
        request.setSubmitterService("secret-rotation-service");
        request.setExecutionRef(Map.of("type", "http"));
        return request;
    }

    private MaintenanceWindowTask activeTask(Long id, String taskType, String workItemId, Long dependsOnTaskId) {
        MaintenanceWindowTask task = new MaintenanceWindowTask();
        task.setId(id);
        task.setAccountId(ACCOUNT_ID);
        task.setResourceCrn(DATAHUB_CRN);
        task.setTaskType(taskType);
        task.setWorkItemId(workItemId);
        task.setStatus(MaintenanceTaskStatus.ACTIVE);
        task.setMaxAttemptsPerOccurrence(1);
        task.setRetryCooldownMinutes(0);
        task.setVersion(1);
        task.setDependsOnTaskId(dependsOnTaskId);
        return task;
    }

    private MaintenanceWindowTaskDependencyRequest dependencyRequest(String resourceCrn, String taskType, String workItemId) {
        MaintenanceWindowTaskDependencyRequest dependsOn = new MaintenanceWindowTaskDependencyRequest();
        dependsOn.setResourceCrn(resourceCrn);
        dependsOn.setTaskType(taskType);
        dependsOn.setWorkItemId(workItemId);
        return dependsOn;
    }
}
