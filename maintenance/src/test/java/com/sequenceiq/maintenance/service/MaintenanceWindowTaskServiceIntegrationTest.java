package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskDependencyRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.request.UpdateMaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;
import com.sequenceiq.maintenance.service.MaintenanceWindowTaskService.TaskRegistrationResult;
import com.sequenceiq.maintenance.util.MaintenanceTaskResourceScope;

@DataJpaTest
@Import({
        MaintenanceWindowTaskService.class,
        MaintenanceWindowTaskRegistrationTxService.class,
        MaintenanceWindowTaskValidator.class,
        MaintenanceWindowTaskConverter.class,
        MaintenanceTaskResourceScope.class
})
@Sql(scripts = "/sql/maintenance_window_task_one_active_per_work_item.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MaintenanceWindowTaskServiceIntegrationTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String ACTOR = "crn:altus:iam:us-west-1:acc-1:user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc-1:datalake:dl-1";

    private static final long NOW = 1_735_689_600_000L;

    @Autowired
    private MaintenanceWindowTaskRepository taskRepository;

    @Autowired
    private MaintenanceWindowTaskService taskService;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        org.mockito.Mockito.when(clock.getCurrentTimeMillis()).thenReturn(NOW);
    }

    @Test
    void registerWithDependsOnResolvesActiveDependency() {
        MaintenanceWindowTaskRequest databaseUpgrade = datalakeRequest();
        databaseUpgrade.setTaskType("DATABASE_UPGRADE");
        databaseUpgrade.setWorkItemId("external-postgres:15");
        TaskRegistrationResult databaseTask = taskService.register(databaseUpgrade, ACCOUNT_ID, ACTOR);

        MaintenanceWindowTaskRequest runtimeUpgrade = datalakeRequest();
        runtimeUpgrade.setTaskType("RUNTIME_UPGRADE");
        runtimeUpgrade.setWorkItemId("runtime:7.2.18");
        MaintenanceWindowTaskDependencyRequest dependsOn = new MaintenanceWindowTaskDependencyRequest();
        dependsOn.setResourceCrn(DATALAKE_CRN);
        dependsOn.setTaskType("DATABASE_UPGRADE");
        dependsOn.setWorkItemId("external-postgres:15");
        runtimeUpgrade.setDependsOn(dependsOn);

        TaskRegistrationResult runtimeTask = taskService.register(runtimeUpgrade, ACCOUNT_ID, ACTOR);

        assertThat(runtimeTask.response().getDependsOn().getTaskType()).isEqualTo("DATABASE_UPGRADE");
        assertThat(taskRepository.findById(runtimeTask.response().getId()).orElseThrow().getDependsOnTaskId())
                .isEqualTo(databaseTask.response().getId());
    }

    @Test
    void duplicateRegisterReturnsExistingActiveRow() {
        MaintenanceWindowTaskRequest request = createRequest();

        TaskRegistrationResult first = taskService.register(request, ACCOUNT_ID, ACTOR);
        TaskRegistrationResult second = taskService.register(request, ACCOUNT_ID, ACTOR);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.response().getId()).isEqualTo(first.response().getId());
        assertThat(taskRepository.findAll()).hasSize(1);
        assertThat(taskRepository.findAll().get(0).getStatus()).isEqualTo(MaintenanceTaskStatus.ACTIVE);
    }

    @Test
    void concurrentRegisterCreatesOnlyOneActiveTask() throws Exception {
        MaintenanceWindowTaskRequest request = createRequest();
        CountDownLatch start = new CountDownLatch(1);
        Callable<TaskRegistrationResult> register = () -> {
            start.await(30, TimeUnit.SECONDS);
            return taskService.register(request, ACCOUNT_ID, ACTOR);
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<TaskRegistrationResult> first = executor.submit(register);
            Future<TaskRegistrationResult> second = executor.submit(register);
            start.countDown();
            TaskRegistrationResult firstResult = first.get(30, TimeUnit.SECONDS);
            TaskRegistrationResult secondResult = second.get(30, TimeUnit.SECONDS);
            List<TaskRegistrationResult> results = List.of(firstResult, secondResult);

            assertThat(taskRepository.findAll()).hasSize(1);
            assertThat(results).extracting(result -> result.response().getId()).containsOnly(firstResult.response().getId());
            assertThat(results.stream().filter(TaskRegistrationResult::created).count()).isEqualTo(1);
            assertThat(results.stream().filter(result -> !result.created()).count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void updateRejectsDependencyCycle() {
        MaintenanceWindowTaskRequest databaseUpgrade = datalakeRequest();
        databaseUpgrade.setTaskType("DATABASE_UPGRADE");
        databaseUpgrade.setWorkItemId("external-postgres:15");
        TaskRegistrationResult databaseTask = taskService.register(databaseUpgrade, ACCOUNT_ID, ACTOR);

        MaintenanceWindowTaskRequest runtimeUpgrade = datalakeRequest();
        runtimeUpgrade.setTaskType("RUNTIME_UPGRADE");
        runtimeUpgrade.setWorkItemId("runtime:7.2.18");
        MaintenanceWindowTaskDependencyRequest dependsOn = new MaintenanceWindowTaskDependencyRequest();
        dependsOn.setResourceCrn(DATALAKE_CRN);
        dependsOn.setTaskType("DATABASE_UPGRADE");
        dependsOn.setWorkItemId("external-postgres:15");
        runtimeUpgrade.setDependsOn(dependsOn);
        TaskRegistrationResult runtimeTask = taskService.register(runtimeUpgrade, ACCOUNT_ID, ACTOR);

        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setDependsOn(dependencyReference(DATALAKE_CRN, "RUNTIME_UPGRADE", "runtime:7.2.18"));

        assertThrows(BadRequestException.class,
                () -> taskService.update(ACCOUNT_ID, databaseTask.response().getId(), request, ACTOR));

        assertThat(taskRepository.findById(runtimeTask.response().getId()).orElseThrow().getDependsOnTaskId())
                .isEqualTo(databaseTask.response().getId());
    }

    @Test
    void disableAndDeleteRemoveTaskFromActiveDispatcherSet() {
        TaskRegistrationResult created = taskService.register(createRequest(), ACCOUNT_ID, ACTOR);
        Long taskId = created.response().getId();

        MaintenanceWindowTaskResponse disabled = taskService.update(ACCOUNT_ID, taskId, disableRequest(), ACTOR);
        assertThat(disabled.getStatus()).isEqualTo("DISABLED");
        assertThat(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).isEmpty();

        taskService.delete(ACCOUNT_ID, taskId, ACTOR);
        assertThat(taskRepository.findById(taskId)).get()
                .extracting(task -> task.getStatus())
                .isEqualTo(MaintenanceTaskStatus.DELETED);
    }

    private MaintenanceWindowTaskRequest datalakeRequest() {
        MaintenanceWindowTaskRequest request = createRequest();
        request.setResourceCrn(DATALAKE_CRN);
        return request;
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

    private UpdateMaintenanceWindowTaskRequest disableRequest() {
        UpdateMaintenanceWindowTaskRequest request = new UpdateMaintenanceWindowTaskRequest();
        request.setStatus("DISABLED");
        return request;
    }

    private MaintenanceWindowTaskDependencyRequest dependencyReference(String resourceCrn, String taskType, String workItemId) {
        MaintenanceWindowTaskDependencyRequest dependsOn = new MaintenanceWindowTaskDependencyRequest();
        dependsOn.setResourceCrn(resourceCrn);
        dependsOn.setTaskType(taskType);
        dependsOn.setWorkItemId(workItemId);
        return dependsOn;
    }
}
