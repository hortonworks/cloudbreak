package com.sequenceiq.maintenance.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterDispatchResult;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterOutcome;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchEvaluation;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchEvaluationRequest;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchSkipReason;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleEligibility;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleEligibilityService;
import com.sequenceiq.maintenance.service.model.MaintenanceWindowResourceIdentity;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;
import com.sequenceiq.maintenance.util.MaintenanceTaskResourceScope;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowDispatchTickServiceTest {

    private static final long NOW = Instant.parse("2026-01-05T16:00:00Z").toEpochMilli();

    private static final long WINDOW_START = NOW;

    private static final long WINDOW_END = Instant.parse("2026-01-05T20:00:00Z").toEpochMilli();

    private static final String POLICY_REVISION = "42:v3";

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:acc-1:datalake:dl-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    @Mock
    private MaintenanceWindowTaskRepository taskRepository;

    @Mock
    private MaintenanceWindowScheduleEligibilityService scheduleEligibilityService;

    @Mock
    private MaintenanceWindowTaskDispatchEvaluator dispatchEvaluator;

    @Mock
    private MaintenanceWindowRunService runService;

    @Mock
    private MaintenanceTaskSubmitterClient submitterClient;

    @Mock
    private MaintenanceTaskResourceScope resourceScope;

    @Mock
    private Clock clock;

    @InjectMocks
    private MaintenanceWindowDispatchTickService underTest;

    private MaintenanceWindowTask task;

    private MaintenanceWindowSchedule schedule;

    private WindowOccurrence occurrence;

    @BeforeEach
    void setUp() {
        task = task();
        schedule = schedule();
        occurrence = new WindowOccurrence(WINDOW_START, WINDOW_END);
        lenient().when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        lenient().when(resourceScope.scopeTypeFromResourceCrn(task.getResourceCrn())).thenReturn(MaintenanceScopeType.DATALAKE);
    }

    @Test
    void tickDoesNothingWhenNoActiveTasks() {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of());

        underTest.tick();

        verify(scheduleEligibilityService, never()).checkEligibility(any(), eq(NOW));
    }

    @Test
    void tickSkipsTaskWhenNoScheduleConfigured() {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenReturn(MaintenanceWindowScheduleEligibility.notDispatchable());

        underTest.tick();

        verify(dispatchEvaluator, never()).evaluate(any());
        verify(runService, never()).recordSkipped(any(), any(), any(), any(), any());
        verify(runService, never()).markRunning(any(), any(), any(), any(), any());
        verify(submitterClient, never()).invokeExecuteCallback(any(), any(), any(), any(), any());
    }

    @Test
    void tickSkipsTaskWhenOutsideActiveWindow() {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenReturn(MaintenanceWindowScheduleEligibility.withoutActiveOccurrence(schedule));

        underTest.tick();

        verify(dispatchEvaluator, never()).evaluate(any());
        verify(runService, never()).recordSkipped(any(), any(), any(), any(), any());
        verify(runService, never()).markRunning(any(), any(), any(), any(), any());
        verify(submitterClient, never()).invokeExecuteCallback(any(), any(), any(), any(), any());
    }

    @Test
    void tickRecordsSkippedWhenScheduleOccurrenceIsSkipped() {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenReturn(MaintenanceWindowScheduleEligibility.skippedOccurrence(schedule, occurrence));

        underTest.tick();

        verify(runService).recordSkipped(task, schedule, occurrence, POLICY_REVISION);
        verify(dispatchEvaluator, never()).evaluate(any());
    }

    @Test
    void tickDefersWhenEvaluatorBlocksOnDependency() {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenReturn(MaintenanceWindowScheduleEligibility.dispatchable(schedule, occurrence));
        when(dispatchEvaluator.evaluate(any(TaskDispatchEvaluationRequest.class)))
                .thenReturn(TaskDispatchEvaluation.skip(
                        TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED, occurrence, null));

        underTest.tick();

        verify(runService, never()).markRunning(any(), any(), any(), any(), any());
        verify(runService, never()).recordSkipped(any(), any(), any(), any(), any());
    }

    @Test
    void tickRecordsSkippedWhenEvaluatorReportsWindowEnded() {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenReturn(MaintenanceWindowScheduleEligibility.dispatchable(schedule, occurrence));
        when(dispatchEvaluator.evaluate(any(TaskDispatchEvaluationRequest.class)))
                .thenReturn(TaskDispatchEvaluation.skip(TaskDispatchSkipReason.WINDOW_ENDED, occurrence, null));

        underTest.tick();

        verify(runService).recordSkipped(task, schedule, occurrence, POLICY_REVISION, TaskDispatchSkipReason.WINDOW_ENDED);
        verify(runService, never()).markRunning(any(), any(), any(), any(), any());
    }

    @Test
    void tickRecordsSkippedWhenEvaluatorReportsTerminalDependencySkip() {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenReturn(MaintenanceWindowScheduleEligibility.dispatchable(schedule, occurrence));
        when(dispatchEvaluator.evaluate(any(TaskDispatchEvaluationRequest.class)))
                .thenReturn(TaskDispatchEvaluation.skip(TaskDispatchSkipReason.DEPENDENCY_SKIPPED, occurrence, null));

        underTest.tick();

        verify(runService).recordSkipped(task, schedule, occurrence, POLICY_REVISION, TaskDispatchSkipReason.DEPENDENCY_SKIPPED);
        verify(runService, never()).markRunning(any(), any(), any(), any(), any());
    }

    static Stream<Arguments> submitterOutcomes() {
        return Stream.of(
                Arguments.of(MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.SYNC_COMPLETED)),
                Arguments.of(MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.ASYNC_ACCEPTED)),
                Arguments.of(MaintenanceTaskSubmitterDispatchResult.failed("submitter unreachable")));
    }

    @ParameterizedTest
    @MethodSource("submitterOutcomes")
    void tickDispatchesEligibleTaskAndAppliesSubmitterOutcome(MaintenanceTaskSubmitterDispatchResult result) {
        MaintenanceWindowRun run = run();
        stubDispatchable(null);
        when(runService.markRunning(task, schedule, occurrence, POLICY_REVISION, null)).thenReturn(run);
        when(submitterClient.invokeExecuteCallback(task, run, schedule, occurrence, POLICY_REVISION)).thenReturn(result);

        underTest.tick();

        verify(runService).markRunning(task, schedule, occurrence, POLICY_REVISION, null);
        verify(submitterClient).invokeExecuteCallback(task, run, schedule, occurrence, POLICY_REVISION);
        verify(runService).applySubmitterOutcome(task, run, result);
    }

    @Test
    void tickPassesPriorRunToMarkRunningOnRetryPath() {
        MaintenanceWindowRun priorRun = run();
        priorRun.setStatus(MaintenanceRunStatus.FAILED);
        priorRun.setAttemptCount(2);
        MaintenanceWindowRun newRun = run();
        newRun.setAttemptCount(3);
        stubDispatchable(priorRun);
        when(runService.markRunning(task, schedule, occurrence, POLICY_REVISION, priorRun)).thenReturn(newRun);
        when(submitterClient.invokeExecuteCallback(task, newRun, schedule, occurrence, POLICY_REVISION))
                .thenReturn(MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.ASYNC_ACCEPTED));

        underTest.tick();

        verify(runService).markRunning(task, schedule, occurrence, POLICY_REVISION, priorRun);
    }

    @Test
    void tickBuildsIdentityFromTaskFields() {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenReturn(MaintenanceWindowScheduleEligibility.notDispatchable());

        underTest.tick();

        ArgumentCaptor<MaintenanceWindowResourceIdentity> identityCaptor =
                ArgumentCaptor.forClass(MaintenanceWindowResourceIdentity.class);
        verify(scheduleEligibilityService).checkEligibility(identityCaptor.capture(), eq(NOW));
        MaintenanceWindowResourceIdentity identity = identityCaptor.getValue();
        assertThat(identity.accountId()).isEqualTo("acc-1");
        assertThat(identity.environmentCrn()).isEqualTo(ENV_CRN);
        assertThat(identity.resourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(identity.resourceScopeType()).isEqualTo(MaintenanceScopeType.DATALAKE);
    }

    @Test
    void tickContinuesAfterTaskFailure() {
        MaintenanceWindowTask secondTask = task();
        secondTask.setId(8L);
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE))
                .thenReturn(List.of(task, secondTask));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(MaintenanceWindowScheduleEligibility.dispatchable(schedule, occurrence));
        when(dispatchEvaluator.evaluate(any(TaskDispatchEvaluationRequest.class)))
                .thenReturn(TaskDispatchEvaluation.dispatch(occurrence, null));
        MaintenanceWindowRun run = run();
        when(runService.markRunning(secondTask, schedule, occurrence, POLICY_REVISION, null)).thenReturn(run);
        when(submitterClient.invokeExecuteCallback(secondTask, run, schedule, occurrence, POLICY_REVISION))
                .thenReturn(MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.SYNC_COMPLETED));

        underTest.tick();

        verify(scheduleEligibilityService, times(2)).checkEligibility(any(), eq(NOW));
        verify(runService).markRunning(secondTask, schedule, occurrence, POLICY_REVISION, null);
    }

    @ParameterizedTest
    @EnumSource(value = TaskDispatchSkipReason.class,
            names = {"WINDOW_ENDED", "DEPENDENCY_SKIPPED", "DEPENDENCY_FAILED", "IMPLICIT_PREREQUISITE_SKIPPED",
                    "IMPLICIT_PREREQUISITE_FAILED"})
    void recordsTerminalSkippedRunClassifiesTerminalReasonsAsTerminal(TaskDispatchSkipReason reason) {
        assertThat(MaintenanceWindowDispatchTickService.recordsTerminalSkippedRun(reason)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TaskDispatchSkipReason.class,
            names = {"DEPENDENCY_NOT_FOUND", "DEPENDENCY_SCOPE_MISMATCH", "DEPENDENCY_NOT_COMPLETED",
                    "IMPLICIT_PLATFORM_ORDERING", "DEDUP_RUNNING", "DEDUP_COMPLETED", "DEDUP_SKIPPED",
                    "RETRY_NOT_ALLOWED", "RETRY_COOLDOWN"})
    void recordsTerminalSkippedRunClassifiesDeferralReasonsAsNonTerminal(TaskDispatchSkipReason reason) {
        assertThat(MaintenanceWindowDispatchTickService.recordsTerminalSkippedRun(reason)).isFalse();
    }

    private void stubDispatchable(MaintenanceWindowRun priorRun) {
        when(taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE)).thenReturn(List.of(task));
        when(scheduleEligibilityService.checkEligibility(any(), eq(NOW)))
                .thenReturn(MaintenanceWindowScheduleEligibility.dispatchable(schedule, occurrence));
        when(dispatchEvaluator.evaluate(any(TaskDispatchEvaluationRequest.class)))
                .thenReturn(TaskDispatchEvaluation.dispatch(occurrence, priorRun));
    }

    private MaintenanceWindowTask task() {
        MaintenanceWindowTask entity = new MaintenanceWindowTask();
        entity.setId(7L);
        entity.setAccountId("acc-1");
        entity.setResourceCrn(RESOURCE_CRN);
        entity.setEnvironmentCrn(ENV_CRN);
        entity.setTaskType("TASK_A");
        entity.setWorkItemId("task-a");
        entity.setTaskKind(MaintenanceTaskKind.EVERY_WINDOW);
        entity.setStatus(MaintenanceTaskStatus.ACTIVE);
        return entity;
    }

    private MaintenanceWindowSchedule schedule() {
        MaintenanceWindowSchedule entity = new MaintenanceWindowSchedule();
        entity.setId(42L);
        entity.setVersion(3);
        return entity;
    }

    private MaintenanceWindowRun run() {
        MaintenanceWindowRun entity = new MaintenanceWindowRun();
        entity.setId(99L);
        entity.setStatus(MaintenanceRunStatus.RUNNING);
        entity.setAttemptCount(1);
        return entity;
    }
}
