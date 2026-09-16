package com.sequenceiq.maintenance.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterDispatchResult;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterOutcome;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchSkipReason;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.repository.MaintenanceWindowRunRepository;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowRunServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final long NOW = Instant.parse("2026-01-05T16:00:00Z").toEpochMilli();

    private static final long WINDOW_START = NOW;

    private static final long WINDOW_END = Instant.parse("2026-01-05T20:00:00Z").toEpochMilli();

    private static final String INITIAL_POLICY_REVISION = "42:v1";

    private static final String UPDATED_POLICY_REVISION = "42:v5";

    @Mock
    private MaintenanceWindowRunRepository runRepository;

    @Mock
    private MaintenanceWindowTaskRepository taskRepository;

    @Mock
    private Clock clock;

    private MaintenanceWindowRunService underTest;

    private MaintenanceWindowTask task;

    private MaintenanceWindowSchedule schedule;

    private WindowOccurrence occurrence;

    @BeforeEach
    void setUp() {
        underTest = new MaintenanceWindowRunService(runRepository, taskRepository, clock);
        lenient().when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        task = task(MaintenanceTaskKind.EVERY_WINDOW);
        schedule = schedule();
        occurrence = new WindowOccurrence(WINDOW_START, WINDOW_END);
        lenient().when(runRepository.saveAndFlush(any(MaintenanceWindowRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void markRunningCreatesNewRunWithAttemptCountOne() {
        MaintenanceWindowRun run = underTest.markRunning(task, schedule, occurrence, INITIAL_POLICY_REVISION, null);

        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.RUNNING);
        assertThat(run.getAttemptCount()).isEqualTo(1);
        assertThat(run.getWindowExecutionStart()).isEqualTo(NOW);
        assertThat(run.getPolicyRevision()).isEqualTo(INITIAL_POLICY_REVISION);
    }

    @Test
    void markRunningIncrementsAttemptCountAfterFailedRetry() {
        MaintenanceWindowRun failed = priorRun(MaintenanceRunStatus.FAILED);
        failed.setAttemptCount(2);
        failed.setPolicyRevision(INITIAL_POLICY_REVISION);
        long firstAttemptStart = NOW - 60_000L;
        failed.setWindowExecutionStart(firstAttemptStart);

        MaintenanceWindowRun run = underTest.markRunning(task, schedule, occurrence, UPDATED_POLICY_REVISION, failed);

        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.RUNNING);
        assertThat(run.getAttemptCount()).isEqualTo(3);
        assertThat(run.getWindowExecutionStart()).isEqualTo(firstAttemptStart);
        assertThat(run.getPolicyRevision()).isEqualTo(INITIAL_POLICY_REVISION);
    }

    @Test
    void markRunningReusesPlannedRunWithoutIncrementingAttemptCount() {
        MaintenanceWindowRun planned = priorRun(MaintenanceRunStatus.PLANNED);
        planned.setAttemptCount(1);

        MaintenanceWindowRun run = underTest.markRunning(task, schedule, occurrence, "42:v1", planned);

        assertThat(run.getAttemptCount()).isEqualTo(1);
        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.RUNNING);
    }

    @Test
    void markRunningRejectsMismatchedTaskId() {
        MaintenanceWindowRun priorRun = priorRun(MaintenanceRunStatus.FAILED);
        MaintenanceWindowTask otherTask = task(MaintenanceTaskKind.EVERY_WINDOW);
        otherTask.setId(999L);
        priorRun.setMaintenanceWindowTask(otherTask);

        assertThatThrownBy(() -> underTest.markRunning(task, schedule, occurrence, "42:v1", priorRun))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("run does not belong to task");
    }

    @Test
    void markRunningRejectsMismatchedWindowStart() {
        MaintenanceWindowRun priorRun = priorRun(MaintenanceRunStatus.FAILED);
        priorRun.setWindowStart(WINDOW_START + 1);

        assertThatThrownBy(() -> underTest.markRunning(task, schedule, occurrence, "42:v1", priorRun))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priorRun does not belong to occurrence");
    }

    @Test
    void markRunningRejectsPriorRunAlreadyRunning() {
        MaintenanceWindowRun running = priorRun(MaintenanceRunStatus.RUNNING);

        assertThatThrownBy(() -> underTest.markRunning(task, schedule, occurrence, "42:v1", running))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot transition to RUNNING");
    }

    @Test
    void markRunningReturnsExistingRowWhenAlreadyPresent() {
        MaintenanceWindowRun existing = priorRun(MaintenanceRunStatus.FAILED);
        existing.setAttemptCount(2);
        existing.setPolicyRevision(INITIAL_POLICY_REVISION);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(task.getId(), WINDOW_START))
                .thenReturn(Optional.of(existing));

        MaintenanceWindowRun run = underTest.markRunning(task, schedule, occurrence, UPDATED_POLICY_REVISION, null);

        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.RUNNING);
        assertThat(run.getAttemptCount()).isEqualTo(3);
        assertThat(run.getPolicyRevision()).isEqualTo(INITIAL_POLICY_REVISION);
    }

    @Test
    void markRunningRecoversWhenConcurrentInsertWinsRace() {
        MaintenanceWindowRun existing = priorRun(MaintenanceRunStatus.RUNNING);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(task.getId(), WINDOW_START))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(runRepository.saveAndFlush(any(MaintenanceWindowRun.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        MaintenanceWindowRun run = underTest.markRunning(task, schedule, occurrence, "42:v1", null);

        assertThat(run).isSameAs(existing);
    }

    @Test
    void recordSkippedCreatesSkippedRunAndKeepsOneShotTaskActive() {
        MaintenanceWindowTask oneShot = task(MaintenanceTaskKind.ONE_SHOT);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(oneShot.getId(), WINDOW_START)).thenReturn(Optional.empty());

        MaintenanceWindowRun run = underTest.recordSkipped(
                oneShot, schedule, occurrence, "42:v1", TaskDispatchSkipReason.DEPENDENCY_SKIPPED);

        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.SKIPPED);
        assertThat(run.getSkipReason()).isEqualTo(TaskDispatchSkipReason.DEPENDENCY_SKIPPED.name());
        verify(taskRepository, never()).saveAndFlush(oneShot);
        assertThat(oneShot.getStatus()).isEqualTo(MaintenanceTaskStatus.ACTIVE);
    }

    @Test
    void recordSkippedIsIdempotentWhenRunExists() {
        MaintenanceWindowRun existing = priorRun(MaintenanceRunStatus.SKIPPED);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(task.getId(), WINDOW_START))
                .thenReturn(Optional.of(existing));

        MaintenanceWindowRun run = underTest.recordSkipped(
                task, schedule, occurrence, "42:v1", TaskDispatchSkipReason.DEPENDENCY_SKIPPED);

        assertThat(run).isSameAs(existing);
        verify(runRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordSkippedDoesNotOverwriteExistingNonSkippedRun() {
        MaintenanceWindowRun existing = priorRun(MaintenanceRunStatus.RUNNING);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(task.getId(), WINDOW_START))
                .thenReturn(Optional.of(existing));

        MaintenanceWindowRun run = underTest.recordSkipped(task, schedule, occurrence, "42:v1");

        assertThat(run).isSameAs(existing);
        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.RUNNING);
        verify(runRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordSkippedRecoversWhenConcurrentInsertWinsRace() {
        MaintenanceWindowRun existing = priorRun(MaintenanceRunStatus.SKIPPED);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(task.getId(), WINDOW_START))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(runRepository.saveAndFlush(any(MaintenanceWindowRun.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        MaintenanceWindowRun run = underTest.recordSkipped(
                task, schedule, occurrence, "42:v1", TaskDispatchSkipReason.DEPENDENCY_SKIPPED);

        assertThat(run).isSameAs(existing);
    }

    @Test
    void applySubmitterOutcomeSyncCompletedMarksRunAndCompletesOneShotTask() {
        MaintenanceWindowTask oneShot = task(MaintenanceTaskKind.ONE_SHOT);
        MaintenanceWindowRun run = priorRun(MaintenanceRunStatus.RUNNING);
        run.setMaintenanceWindowTask(oneShot);

        underTest.applySubmitterOutcome(oneShot, run,
                MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.SYNC_COMPLETED));

        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.COMPLETED);
        assertThat(run.getWindowExecutionEnd()).isEqualTo(NOW);
        verify(taskRepository).saveAndFlush(oneShot);
    }

    @Test
    void applySubmitterOutcomeAsyncAcceptedKeepsRunRunning() {
        MaintenanceWindowRun run = priorRun(MaintenanceRunStatus.RUNNING);

        underTest.applySubmitterOutcome(task, run,
                MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.ASYNC_ACCEPTED));

        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.RUNNING);
        assertThat(run.getWindowExecutionEnd()).isNull();
        assertThat(run.getUpdatedAt()).isEqualTo(NOW);
        verify(runRepository).saveAndFlush(run);
    }

    @Test
    void applySubmitterOutcomeFailedTerminalOneShotWithoutRetry() {
        MaintenanceWindowTask oneShot = task(MaintenanceTaskKind.ONE_SHOT);
        MaintenanceWindowRun run = priorRun(MaintenanceRunStatus.RUNNING);
        run.setMaintenanceWindowTask(oneShot);
        run.setAttemptCount(1);

        underTest.applySubmitterOutcome(oneShot, run, MaintenanceTaskSubmitterDispatchResult.failed("boom"));

        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.FAILED);
        assertThat(run.getErrorDetail()).isEqualTo("boom");
        verify(taskRepository).saveAndFlush(oneShot);
    }

    @Test
    void applySubmitterOutcomeFailedKeepsOneShotActiveWhenRetriesRemain() {
        MaintenanceWindowTask oneShot = task(MaintenanceTaskKind.ONE_SHOT);
        oneShot.setRetryWithinOccurrence(true);
        oneShot.setMaxAttemptsPerOccurrence(3);
        MaintenanceWindowRun run = priorRun(MaintenanceRunStatus.RUNNING);
        run.setMaintenanceWindowTask(oneShot);
        run.setAttemptCount(1);

        underTest.applySubmitterOutcome(oneShot, run, MaintenanceTaskSubmitterDispatchResult.failed("boom"));

        assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.FAILED);
        verify(taskRepository, never()).saveAndFlush(oneShot);
        assertThat(oneShot.getStatus()).isEqualTo(MaintenanceTaskStatus.ACTIVE);
    }

    @Test
    void applySubmitterOutcomeRejectsMismatchedRun() {
        MaintenanceWindowRun run = priorRun(MaintenanceRunStatus.RUNNING);
        MaintenanceWindowTask otherTask = task(MaintenanceTaskKind.EVERY_WINDOW);
        otherTask.setId(999L);
        run.setMaintenanceWindowTask(otherTask);

        assertThatThrownBy(() -> underTest.applySubmitterOutcome(task, run, MaintenanceTaskSubmitterDispatchResult.failed("boom")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("run does not belong to task");
    }

    @Test
    void completeRunMarksCompletedAndCompletesOneShotTask() {
        MaintenanceWindowTask oneShot = task(MaintenanceTaskKind.ONE_SHOT);
        MaintenanceWindowRun run = priorRun(MaintenanceRunStatus.RUNNING);
        run.setId(99L);
        run.setMaintenanceWindowTask(oneShot);
        when(runRepository.findByIdAndMaintenanceWindowTaskIdAndAccountId(99L, oneShot.getId(), ACCOUNT_ID))
                .thenReturn(Optional.of(run));
        when(taskRepository.findByIdAndAccountId(oneShot.getId(), ACCOUNT_ID)).thenReturn(Optional.of(oneShot));

        MaintenanceWindowRun saved = underTest.completeRun(ACCOUNT_ID, 99L, oneShot.getId());

        assertThat(saved.getStatus()).isEqualTo(MaintenanceRunStatus.COMPLETED);
        verify(taskRepository).saveAndFlush(oneShot);
    }

    @Test
    void completeRunRejectsMismatchedAccountId() {
        when(runRepository.findByIdAndMaintenanceWindowTaskIdAndAccountId(99L, task.getId(), "other-account"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.completeRun("other-account", 99L, task.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completeRunRejectsNonRunningStatus() {
        MaintenanceWindowRun completed = priorRun(MaintenanceRunStatus.COMPLETED);
        completed.setId(99L);
        when(runRepository.findByIdAndMaintenanceWindowTaskIdAndAccountId(99L, task.getId(), ACCOUNT_ID))
                .thenReturn(Optional.of(completed));
        when(taskRepository.findByIdAndAccountId(task.getId(), ACCOUNT_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> underTest.completeRun(ACCOUNT_ID, 99L, task.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not RUNNING");
    }

    @Test
    void policyRevisionFormatsScheduleIdAndVersion() {
        assertThat(MaintenanceWindowRunService.policyRevision(schedule)).isEqualTo("42:v3");
    }

    private MaintenanceWindowTask task(MaintenanceTaskKind taskKind) {
        MaintenanceWindowTask entity = new MaintenanceWindowTask();
        entity.setId(7L);
        entity.setAccountId(ACCOUNT_ID);
        entity.setResourceCrn("crn:cdp:datalake:us-west-1:acc-1:datalake:dl-1");
        entity.setEnvironmentCrn("crn:cdp:environments:us-west-1:acc-1:environment:env-1");
        entity.setTaskType("TASK_A");
        entity.setWorkItemId("task-a");
        entity.setTaskKind(taskKind);
        entity.setStatus(MaintenanceTaskStatus.ACTIVE);
        entity.setRetryWithinOccurrence(false);
        entity.setMaxAttemptsPerOccurrence(1);
        return entity;
    }

    private MaintenanceWindowSchedule schedule() {
        MaintenanceWindowSchedule entity = new MaintenanceWindowSchedule();
        entity.setId(42L);
        entity.setVersion(3);
        return entity;
    }

    private MaintenanceWindowRun priorRun(MaintenanceRunStatus status) {
        MaintenanceWindowRun run = new MaintenanceWindowRun();
        run.setMaintenanceWindowTask(task);
        run.setAccountId(task.getAccountId());
        run.setResourceCrn(task.getResourceCrn());
        run.setWindowStart(WINDOW_START);
        run.setWindowEnd(WINDOW_END);
        run.setStatus(status);
        run.setAttemptCount(1);
        return run;
    }
}
