package com.sequenceiq.maintenance.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchEvaluation;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchEvaluationRequest;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchSkipReason;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.repository.MaintenanceWindowRunRepository;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;
import com.sequenceiq.maintenance.util.MaintenanceTaskResourceScope;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowTaskDispatchEvaluatorTest {

    private static final Instant WINDOW_START = Instant.parse("2026-01-05T16:00:00Z");

    private static final Instant WINDOW_END = Instant.parse("2026-01-05T20:00:00Z");

    private static final long WINDOW_START_MS = WINDOW_START.toEpochMilli();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc-1:datalake:dl-1";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:acc-1:freeipa:fp-1";

    @Mock
    private MaintenanceWindowRunRepository runRepository;

    @Mock
    private Clock clock;

    private MaintenanceWindowTaskDispatchEvaluator underTest;

    private WindowOccurrence occurrence;

    @BeforeEach
    void setUp() {
        underTest = new MaintenanceWindowTaskDispatchEvaluator(
                runRepository, clock, new MaintenanceTaskResourceScope());
        occurrence = new WindowOccurrence(WINDOW_START_MS, WINDOW_END.toEpochMilli());
        lenient().when(runRepository
                .findFirstByMaintenanceWindowTaskIdAndWindowStartLessThanAndWindowEndGreaterThanOrderByWindowStartDesc(
                        anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());
    }

    @Test
    void waitsWhenPrerequisiteHasNoOverlappingRun() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED);
    }

    @Test
    void waitsWhenDependencyIsRunning() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        MaintenanceWindowRun running = completedRun(dependency);
        running.setStatus(MaintenanceRunStatus.RUNNING);
        stubOverlappingPrerequisiteRun(1L, occurrence, running);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED);
    }

    @Test
    void waitsWhenDependencyIsPlanned() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        MaintenanceWindowRun planned = completedRun(dependency);
        planned.setStatus(MaintenanceRunStatus.PLANNED);
        stubOverlappingPrerequisiteRun(1L, occurrence, planned);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED);
    }

    @Test
    void skipsWhenDependencyTerminallyFailed() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        MaintenanceWindowRun failed = completedRun(dependency);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        stubOverlappingPrerequisiteRun(1L, occurrence, failed);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_FAILED);
    }

    @Test
    void waitsWhenDependencyFailedButRetryStillAllowed() {
        stubNow(WINDOW_START.plus(60, ChronoUnit.MINUTES));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        dependency.setRetryWithinOccurrence(true);
        dependency.setMaxAttemptsPerOccurrence(3);
        dependency.setRetryCooldownMinutes(0);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        MaintenanceWindowRun failed = completedRun(dependency);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        failed.setAttemptCount(2);
        stubOverlappingPrerequisiteRun(1L, occurrence, failed);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED);
    }

    @Test
    void skipsWhenDependencySkipped() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        MaintenanceWindowRun skipped = completedRun(dependency);
        skipped.setStatus(MaintenanceRunStatus.SKIPPED);
        stubOverlappingPrerequisiteRun(1L, occurrence, skipped);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_SKIPPED);
    }

    @Test
    void skipsWhenDependencyNotFound() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 99L, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_NOT_FOUND);
    }

    @Test
    void skipsWhenDependencyAbsentFromActiveTasks() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_NOT_FOUND);
    }

    @Test
    void skipsWhenDependencyInDifferentAccount() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        dependency.setAccountId("acc-2");
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_SCOPE_MISMATCH);
    }

    @Test
    void skipsWhenDependencyInDifferentEnvironment() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        dependency.setEnvironmentCrn("crn:cdp:environments:us-west-1:acc-1:environment:env-2");
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_SCOPE_MISMATCH);
    }

    @Test
    void dispatchesWhenDependencyCompleted() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, DATALAKE_CRN);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        stubOverlappingPrerequisiteRun(1L, occurrence, completedRun(dependency));
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(dependent, List.of(dependency, dependent));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void dispatchesWhenPriorRunIsPlanned() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        MaintenanceWindowRun planned = completedRun(task);
        planned.setStatus(MaintenanceRunStatus.PLANNED);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(planned));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void dedupSkipsCompletedRun() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(completedRun(task)));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEDUP_COMPLETED);
    }

    @Test
    void dedupSkipsRunningRun() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        MaintenanceWindowRun running = completedRun(task);
        running.setStatus(MaintenanceRunStatus.RUNNING);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(running));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEDUP_RUNNING);
    }

    @Test
    void dedupSkipsSkippedRun() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        MaintenanceWindowRun skipped = completedRun(task);
        skipped.setStatus(MaintenanceRunStatus.SKIPPED);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(skipped));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEDUP_SKIPPED);
    }

    @Test
    void allowsTwoDatalakeTasksInSameEnvironmentToDispatchConcurrently() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask taskA = task(20L, "TASK_A", 100, null, DATALAKE_CRN);
        MaintenanceWindowTask taskB = task(21L, "TASK_B", 50, null, DATALAKE_CRN);
        taskB.setWorkItemId("task_b");
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(20L, WINDOW_START_MS)).thenReturn(Optional.empty());
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(21L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluationA = evaluate(taskA, List.of(taskA, taskB));
        TaskDispatchEvaluation evaluationB = evaluate(taskB, List.of(taskA, taskB));

        assertThat(evaluationA.shouldDispatch()).isTrue();
        assertThat(evaluationB.shouldDispatch()).isTrue();
    }

    @Test
    void dispatchesWhenEvaluatedBeforeWindowStart() {
        stubNow(WINDOW_START.minusSeconds(1));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void allowsRedispatchAfterFailedWhenRetryEnabledAndCooldownElapsed() {
        stubNow(WINDOW_START.plus(46, ChronoUnit.MINUTES));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        task.setRetryWithinOccurrence(true);
        task.setMaxAttemptsPerOccurrence(3);
        task.setRetryCooldownMinutes(15);
        MaintenanceWindowRun failed = completedRun(task);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        failed.setWindowExecutionEnd(WINDOW_START_MS + TimeUnit.MINUTES.toMillis(30));
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(failed));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void skipsFailedRedispatchDuringCooldown() {
        stubNow(WINDOW_START.plus(40, ChronoUnit.MINUTES));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        task.setRetryWithinOccurrence(true);
        task.setMaxAttemptsPerOccurrence(3);
        task.setRetryCooldownMinutes(15);
        MaintenanceWindowRun failed = completedRun(task);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        failed.setWindowExecutionEnd(WINDOW_START_MS + TimeUnit.MINUTES.toMillis(30));
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(failed));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.RETRY_COOLDOWN);
    }

    @Test
    void skipsFailedRedispatchWhenWindowExecutionEndMissingAndCooldownConfigured() {
        stubNow(WINDOW_START.plus(60, ChronoUnit.MINUTES));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        task.setRetryWithinOccurrence(true);
        task.setMaxAttemptsPerOccurrence(3);
        task.setRetryCooldownMinutes(15);
        MaintenanceWindowRun failed = completedRun(task);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        failed.setWindowExecutionEnd(null);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(failed));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.RETRY_COOLDOWN);
    }

    @Test
    void skipsFailedRedispatchWhenMaxAttemptsExhausted() {
        stubNow(WINDOW_START.plus(60, ChronoUnit.MINUTES));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        task.setRetryWithinOccurrence(true);
        task.setMaxAttemptsPerOccurrence(3);
        task.setRetryCooldownMinutes(0);
        MaintenanceWindowRun failed = completedRun(task);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        failed.setAttemptCount(3);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(failed));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.RETRY_NOT_ALLOWED);
    }

    @Test
    void allowsFailedRedispatchWhenAttemptsRemain() {
        stubNow(WINDOW_START.plus(60, ChronoUnit.MINUTES));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        task.setRetryWithinOccurrence(true);
        task.setMaxAttemptsPerOccurrence(3);
        task.setRetryCooldownMinutes(0);
        MaintenanceWindowRun failed = completedRun(task);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        failed.setAttemptCount(2);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(failed));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void usesAuthoritativeTaskForFailedRetryPolicyNotStaleRunAssociation() {
        stubNow(WINDOW_START.plus(60, ChronoUnit.MINUTES));
        MaintenanceWindowTask authoritativeTask = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        authoritativeTask.setRetryWithinOccurrence(true);
        authoritativeTask.setMaxAttemptsPerOccurrence(5);
        authoritativeTask.setRetryCooldownMinutes(0);
        MaintenanceWindowTask staleTaskSnapshot = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        staleTaskSnapshot.setRetryWithinOccurrence(true);
        staleTaskSnapshot.setMaxAttemptsPerOccurrence(1);
        staleTaskSnapshot.setRetryCooldownMinutes(0);
        MaintenanceWindowRun failed = completedRun(staleTaskSnapshot);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        failed.setAttemptCount(2);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(failed));

        TaskDispatchEvaluation evaluation = evaluate(authoritativeTask, List.of(authoritativeTask));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void failedRetryPolicyDoesNotLoadTaskFromPriorRun() {
        stubNow(WINDOW_START.plus(60, ChronoUnit.MINUTES));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        task.setRetryWithinOccurrence(true);
        task.setMaxAttemptsPerOccurrence(3);
        task.setRetryCooldownMinutes(0);
        MaintenanceWindowRun failed = new MaintenanceWindowRun();
        failed.setStatus(MaintenanceRunStatus.FAILED);
        failed.setAttemptCount(2);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(failed));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void skipsFailedRedispatchWhenRetryNotEnabled() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        MaintenanceWindowRun failed = completedRun(task);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS))
                .thenReturn(Optional.of(failed));

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.RETRY_NOT_ALLOWED);
    }

    @Test
    void dispatchesWhenMonthlyPrerequisiteWindowOverlapsWeeklyDependent() {
        WindowOccurrence weeklyDependentOccurrence = new WindowOccurrence(WINDOW_START_MS, WINDOW_END.toEpochMilli());
        stubNow(WINDOW_START.plus(1, ChronoUnit.HOURS));
        MaintenanceWindowTask monthlyPrerequisite = task(1L, "MONTHLY_TASK", 200, null, FREEIPA_CRN);
        MaintenanceWindowTask weeklyDependent = task(2L, "WEEKLY_TASK", 150, 1L, DATALAKE_CRN);
        MaintenanceWindowRun completed = completedRun(monthlyPrerequisite);
        completed.setWindowStart(WINDOW_START_MS - TimeUnit.DAYS.toMillis(28));
        completed.setWindowEnd(WINDOW_START_MS + TimeUnit.DAYS.toMillis(1));
        stubOverlappingPrerequisiteRun(1L, weeklyDependentOccurrence, completed);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = underTest.evaluate(
                new TaskDispatchEvaluationRequest(weeklyDependent, weeklyDependentOccurrence, List.of(monthlyPrerequisite, weeklyDependent)));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void dispatchesWhenDependencyCompletedOnItsOwnOccurrenceWindowStart() {
        long prerequisiteWindowStart = WINDOW_START_MS - TimeUnit.HOURS.toMillis(48);
        WindowOccurrence dependentOccurrence = new WindowOccurrence(WINDOW_START_MS, WINDOW_END.toEpochMilli());
        stubNow(WINDOW_START.plus(1, ChronoUnit.HOURS));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, FREEIPA_CRN);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        MaintenanceWindowRun completed = completedRun(dependency);
        completed.setWindowStart(prerequisiteWindowStart);
        completed.setWindowEnd(WINDOW_START_MS + TimeUnit.HOURS.toMillis(1));
        stubOverlappingPrerequisiteRun(1L, dependentOccurrence, completed);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS))
                .thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = underTest.evaluate(
                new TaskDispatchEvaluationRequest(dependent, dependentOccurrence, List.of(dependency, dependent)));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void skipsWhenPrerequisiteCompletedOutsideTwentyFourHourLookbackButDoesNotOverlap() {
        WindowOccurrence dependentOccurrence = new WindowOccurrence(WINDOW_START_MS, WINDOW_END.toEpochMilli());
        stubNow(WINDOW_START.plus(1, ChronoUnit.HOURS));
        MaintenanceWindowTask dependency = task(1L, "TASK_A", 200, null, FREEIPA_CRN);
        MaintenanceWindowTask dependent = task(2L, "TASK_B", 150, 1L, DATALAKE_CRN);
        MaintenanceWindowRun completed = completedRun(dependency);
        completed.setWindowStart(WINDOW_START_MS - TimeUnit.HOURS.toMillis(48));
        completed.setWindowEnd(WINDOW_START_MS - TimeUnit.HOURS.toMillis(47));
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(2L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = underTest.evaluate(
                new TaskDispatchEvaluationRequest(dependent, dependentOccurrence, List.of(dependency, dependent)));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED);
    }

    @Test
    void implicitOrderingDefersDatalakeWhileFreeIpaIsRunning() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask freeIpa = task(10L, "FREEIPA_TASK", 100, null, FREEIPA_CRN);
        MaintenanceWindowTask datalake = task(11L, "DATALAKE_TASK", 100, null, DATALAKE_CRN);
        MaintenanceWindowRun running = completedRun(freeIpa);
        running.setStatus(MaintenanceRunStatus.RUNNING);
        stubOverlappingPrerequisiteRun(10L, occurrence, running);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(11L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(datalake, List.of(freeIpa, datalake));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.IMPLICIT_PLATFORM_ORDERING);
    }

    @Test
    void implicitOrderingDefersDatalakeWhileFreeIpaIsPlanned() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask freeIpa = task(10L, "FREEIPA_TASK", 100, null, FREEIPA_CRN);
        MaintenanceWindowTask datalake = task(11L, "DATALAKE_TASK", 100, null, DATALAKE_CRN);
        MaintenanceWindowRun planned = completedRun(freeIpa);
        planned.setStatus(MaintenanceRunStatus.PLANNED);
        stubOverlappingPrerequisiteRun(10L, occurrence, planned);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(11L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(datalake, List.of(freeIpa, datalake));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.IMPLICIT_PLATFORM_ORDERING);
    }

    @Test
    void implicitOrderingDefersDatalakeUntilFreeIpaCompletes() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask freeIpa = task(10L, "FREEIPA_TASK", 100, null, FREEIPA_CRN);
        MaintenanceWindowTask datalake = task(11L, "DATALAKE_TASK", 100, null, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(11L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(datalake, List.of(freeIpa, datalake));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.IMPLICIT_PLATFORM_ORDERING);
    }

    @Test
    void implicitOrderingSkipsDatalakeWhenFreeIpaTerminallyFailed() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask freeIpa = task(10L, "FREEIPA_TASK", 100, null, FREEIPA_CRN);
        MaintenanceWindowTask datalake = task(11L, "DATALAKE_TASK", 100, null, DATALAKE_CRN);
        MaintenanceWindowRun failed = completedRun(freeIpa);
        failed.setStatus(MaintenanceRunStatus.FAILED);
        stubOverlappingPrerequisiteRun(10L, occurrence, failed);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(11L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(datalake, List.of(freeIpa, datalake));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.IMPLICIT_PREREQUISITE_FAILED);
    }

    @Test
    void implicitOrderingSkipsDatalakeWhenFreeIpaSkipped() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask freeIpa = task(10L, "FREEIPA_TASK", 100, null, FREEIPA_CRN);
        MaintenanceWindowTask datalake = task(11L, "DATALAKE_TASK", 100, null, DATALAKE_CRN);
        MaintenanceWindowRun skipped = completedRun(freeIpa);
        skipped.setStatus(MaintenanceRunStatus.SKIPPED);
        stubOverlappingPrerequisiteRun(10L, occurrence, skipped);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(11L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(datalake, List.of(freeIpa, datalake));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.IMPLICIT_PREREQUISITE_SKIPPED);
    }

    @Test
    void dispatchesWhenLowerTierTaskHasDependsOnAndIsExcludedFromImplicitOrdering() {
        stubNow(WINDOW_START.plusSeconds(1));
        MaintenanceWindowTask freeIpaWithExplicitDependency = task(10L, "FREEIPA_TASK", 100, 99L, FREEIPA_CRN);
        MaintenanceWindowTask datalake = task(11L, "DATALAKE_TASK", 100, null, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(11L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(datalake, List.of(freeIpaWithExplicitDependency, datalake));

        assertThat(evaluation.shouldDispatch()).isTrue();
    }

    @Test
    void doesNotStartNewTaskAfterWindowEnd() {
        stubNow(WINDOW_END);
        MaintenanceWindowTask task = task(3L, "TASK_C", 50, null, DATALAKE_CRN);
        when(runRepository.findByMaintenanceWindowTaskIdAndWindowStart(3L, WINDOW_START_MS)).thenReturn(Optional.empty());

        TaskDispatchEvaluation evaluation = evaluate(task, List.of(task));

        assertThat(evaluation.shouldDispatch()).isFalse();
        assertThat(evaluation.skipReason()).contains(TaskDispatchSkipReason.WINDOW_ENDED);
    }

    private void stubOverlappingPrerequisiteRun(
            long prerequisiteTaskId, WindowOccurrence dependentOccurrence, MaintenanceWindowRun run) {
        when(runRepository.findFirstByMaintenanceWindowTaskIdAndWindowStartLessThanAndWindowEndGreaterThanOrderByWindowStartDesc(
                eq(prerequisiteTaskId), eq(dependentOccurrence.windowEnd()), eq(dependentOccurrence.windowStart())))
                .thenReturn(Optional.of(run));
    }

    private void stubNow(Instant now) {
        when(clock.getCurrentInstant()).thenReturn(now);
    }

    private TaskDispatchEvaluation evaluate(
            MaintenanceWindowTask task,
            List<MaintenanceWindowTask> activeTasks) {
        return underTest.evaluate(new TaskDispatchEvaluationRequest(task, occurrence, activeTasks));
    }

    private MaintenanceWindowTask task(Long id, String taskType, int priority, Long dependsOn, String resourceCrn) {
        MaintenanceWindowTask task = new MaintenanceWindowTask();
        task.setId(id);
        task.setAccountId("acc-1");
        task.setEnvironmentCrn(ENV_CRN);
        task.setResourceCrn(resourceCrn);
        task.setTaskType(taskType);
        task.setWorkItemId(taskType.toLowerCase());
        task.setTaskKind(MaintenanceTaskKind.EVERY_WINDOW);
        task.setStatus(MaintenanceTaskStatus.ACTIVE);
        task.setSubmitterService("datalake");
        task.setPriority(priority);
        task.setDependsOnTaskId(dependsOn);
        task.setRetryWithinOccurrence(false);
        task.setMaxAttemptsPerOccurrence(1);
        task.setRetryCooldownMinutes(0);
        return task;
    }

    private MaintenanceWindowRun completedRun(MaintenanceWindowTask task) {
        MaintenanceWindowRun run = new MaintenanceWindowRun();
        run.setMaintenanceWindowTask(task);
        run.setStatus(MaintenanceRunStatus.COMPLETED);
        run.setAttemptCount(1);
        return run;
    }
}
