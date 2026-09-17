package com.sequenceiq.maintenance.dispatcher;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowRunRepository;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

/**
 * Persists per-occurrence execution state in {@code maintenance_window_runs} for the maintenance
 * window dispatcher.
 */
@Service
public class MaintenanceWindowRunService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowRunService.class);

    private final MaintenanceWindowRunRepository runRepository;

    private final MaintenanceWindowTaskRepository taskRepository;

    private final Clock clock;

    @Inject
    public MaintenanceWindowRunService(
            MaintenanceWindowRunRepository runRepository,
            MaintenanceWindowTaskRepository taskRepository,
            Clock clock) {
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    /**
     * Creates a new run or transitions an existing row to {@link MaintenanceRunStatus#RUNNING}.
     * Increments {@link MaintenanceWindowRun#getAttemptCount()} only when redispatching after
     * {@link MaintenanceRunStatus#FAILED}; a {@link MaintenanceRunStatus#PLANNED} row keeps its count.
     * Other prior statuses are rejected.
     * <p>
     * Concurrent dispatcher ticks or pods may both see no {@code priorRun} and attempt insert; the unique
     * constraint on {@code (maintenance_window_task_id, window_start)} lets one win and the loser re-reads
     * the existing row (returning it when already {@code RUNNING}, otherwise transitioning it).
     */
    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowRun markRunning(
            MaintenanceWindowTask task,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision,
            MaintenanceWindowRun priorRun) {
        if (priorRun == null) {
            return runRepository.findByMaintenanceWindowTaskIdAndWindowStart(task.getId(), occurrence.windowStart())
                    .map(existing -> saveRunWithRunningStatus(task, schedule, occurrence, existing))
                    .orElseGet(() -> createRunWithRunningStatusIfAbsent(task, schedule, occurrence, policyRevision));
        }
        return saveRunWithRunningStatus(task, schedule, occurrence, priorRun);
    }

    /**
     * Records a terminal {@link MaintenanceRunStatus#SKIPPED} run for a skipped occurrence when none exists yet.
     * {@link MaintenanceTaskKind#ONE_SHOT} tasks stay {@link MaintenanceTaskStatus#ACTIVE} so they may run in a
     * later window; only successful or terminally failed execution completes the registration.
     * <p>
     * Concurrent dispatcher ticks or pods may both miss the row and attempt insert; the unique constraint on
     * {@code (maintenance_window_task_id, window_start)} lets one win and the loser re-reads the existing row.
     */
    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowRun recordSkipped(
            MaintenanceWindowTask task,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision) {
        return recordSkipped(task, schedule, occurrence, policyRevision, null);
    }

    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowRun recordSkipped(
            MaintenanceWindowTask task,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision,
            TaskDispatchSkipReason skipReason) {
        return runRepository.findByMaintenanceWindowTaskIdAndWindowStart(task.getId(), occurrence.windowStart())
                .map(existing -> {
                    logSkippedRunNotWritten(task, occurrence, existing, skipReason);
                    return existing;
                })
                .orElseGet(() -> createSkippedRunIfAbsent(task, schedule, occurrence, policyRevision, skipReason));
    }

    /**
     * Applies a submitter dispatch result to the run and, when appropriate, to the owning task.
     * <p>
     * The caller must invoke {@link #markRunning} beforehand so {@code run} is a managed entity in status
     * {@link MaintenanceRunStatus#RUNNING}; this method verifies both that {@code run} belongs to {@code task}
     * and that its status is still {@code RUNNING} before applying a terminal transition (non-RUNNING runs are
     * rejected with {@link ConflictException} except for idempotent callback retries on {@link #completeRun} /
     * {@link #failRun}).
     * <p>
     * Effects by {@link MaintenanceTaskSubmitterDispatchResult#outcome()}:
     * <ul>
     *   <li>{@code SYNC_COMPLETED} — run → {@link MaintenanceRunStatus#COMPLETED}; if the task is
     *       {@link MaintenanceTaskKind#ONE_SHOT}, task → {@link MaintenanceTaskStatus#COMPLETED}.</li>
     *   <li>{@code ASYNC_ACCEPTED} — run stays {@link MaintenanceRunStatus#RUNNING} awaiting the submitter's
     *       completion callback ({@link #completeRun}); only {@code updated_at} is touched.</li>
     *   <li>{@code FAILED} — run → {@link MaintenanceRunStatus#FAILED} with {@code error_detail} persisted; for
     *       {@link MaintenanceTaskKind#ONE_SHOT}, the task is completed unless {@code retry_within_occurrence}
     *       is set and {@code attempt_count < max_attempts_per_occurrence}.</li>
     * </ul>
     * Throws {@link IllegalStateException} if a future {@link MaintenanceTaskSubmitterOutcome} value is
     * unhandled here — added to prevent silent no-ops when the enum is extended.
     */
    @Transactional(TxType.REQUIRED)
    public void applySubmitterOutcome(
            MaintenanceWindowTask task,
            MaintenanceWindowRun run,
            MaintenanceTaskSubmitterDispatchResult result) {
        validateRunBelongsToTask(run, task);
        switch (result.outcome()) {
            case SYNC_COMPLETED -> applyTerminalOutcome(run, task, MaintenanceRunStatus.COMPLETED, null);
            case ASYNC_ACCEPTED -> {
                requireRunning(run);
                long now = clock.getCurrentTimeMillis();
                run.setUpdatedAt(now);
                runRepository.saveAndFlush(run);
            }
            case FAILED -> applyTerminalOutcome(run, task, MaintenanceRunStatus.FAILED, result.errorDetail());
            default -> throw new IllegalStateException("Unhandled submitter outcome: " + result.outcome());
        }
        LOGGER.debug("Applied submitter outcome {} to taskId={} runId={}", result.outcome(), task.getId(), run.getId());
    }

    /**
     * Marks a run {@link MaintenanceRunStatus#COMPLETED} and completes a {@link MaintenanceTaskKind#ONE_SHOT} task when applicable.
     * Re-reporting {@code COMPLETED} for an already completed run is an idempotent no-op (at-least-once callback retries).
     * <p>
     * Uses two repository reads: {@code findByIdAndMaintenanceWindowTaskIdAndAccountId} validates run ownership and
     * tenant scope without initializing the run's lazy task association; {@code findByIdAndAccountId(taskId)} loads
     * the task for ONE_SHOT completion.
     */
    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowRun completeRun(String accountId, Long runId, Long taskId) {
        return applyCallbackOutcome(accountId, runId, taskId, MaintenanceRunStatus.COMPLETED, null);
    }

    /**
     * Marks a run {@link MaintenanceRunStatus#FAILED} after async submitter work and applies ONE_SHOT task rules
     * consistent with {@link #applySubmitterOutcome} for {@code FAILED}. Re-reporting {@code FAILED} for an already
     * failed run is an idempotent no-op.
     */
    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowRun failRun(String accountId, Long runId, Long taskId, String errorDetail) {
        return applyCallbackOutcome(accountId, runId, taskId, MaintenanceRunStatus.FAILED, errorDetail);
    }

    private MaintenanceWindowRun applyCallbackOutcome(
            String accountId, Long runId, Long taskId, MaintenanceRunStatus terminalStatus, String errorDetail) {
        MaintenanceWindowRun run = runRepository.findByIdAndMaintenanceWindowTaskIdAndAccountId(runId, taskId, accountId)
                .orElseThrow(notFound(String.format(
                        "Maintenance run not found for accountId=%s taskId=%s runId=%s", accountId, taskId, runId)));
        MaintenanceWindowTask task = taskRepository.findByIdAndAccountId(taskId, accountId)
                .orElseThrow(notFound(String.format(
                        "Maintenance task not found for accountId=%s taskId=%s", accountId, taskId)));
        MaintenanceWindowRun saved = applyTerminalOutcome(run, task, terminalStatus, errorDetail);
        LOGGER.debug("Applied {} to maintenance run: taskId={} runId={}", terminalStatus, taskId, runId);
        return saved;
    }

    private MaintenanceWindowRun applyTerminalOutcome(
            MaintenanceWindowRun run,
            MaintenanceWindowTask task,
            MaintenanceRunStatus terminalStatus,
            String errorDetail) {
        if (run.getStatus() == terminalStatus) {
            LOGGER.info("Outcome callback skipped for run id={}: already {}", run.getId(), terminalStatus);
            return run;
        }
        if (run.getStatus() != MaintenanceRunStatus.RUNNING) {
            throw new ConflictException(String.format(
                    "Run %s cannot transition from %s to %s", run.getId(), run.getStatus(), terminalStatus));
        }
        long now = clock.getCurrentTimeMillis();
        run.setStatus(terminalStatus);
        run.setWindowExecutionEnd(now);
        run.setErrorDetail(errorDetail);
        run.setUpdatedAt(now);
        MaintenanceWindowRun saved;
        try {
            saved = runRepository.saveAndFlush(run);
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("Run was modified concurrently.", e);
        }
        if (terminalStatus == MaintenanceRunStatus.COMPLETED) {
            completeTaskIfOneShot(task, now);
        } else {
            completeOneShotIfTerminalFailure(task, run, now);
        }
        return saved;
    }

    private static void requireRunning(MaintenanceWindowRun run) {
        if (run.getStatus() != MaintenanceRunStatus.RUNNING) {
            throw new ConflictException(
                    "Run " + run.getId() + " is not RUNNING (status=" + run.getStatus() + ")");
        }
    }

    public static String policyRevision(MaintenanceWindowSchedule schedule) {
        return schedule.getId() + ":v" + schedule.getVersion();
    }

    private MaintenanceWindowRun createRunWithRunningStatusIfAbsent(
            MaintenanceWindowTask task,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision) {
        try {
            MaintenanceWindowRun saved = saveRunWithRunningStatus(
                    task, schedule, occurrence, newMaintenanceWindowRun(task, occurrence, policyRevision));
            LOGGER.debug("Created maintenance run with status RUNNING: taskId={} runId={}", task.getId(), saved.getId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            MaintenanceWindowRun existing = runRepository.findByMaintenanceWindowTaskIdAndWindowStart(
                            task.getId(), occurrence.windowStart())
                    .orElseThrow(() -> e);
            if (existing.getStatus() == MaintenanceRunStatus.RUNNING) {
                LOGGER.debug("Concurrent markRunning lost insert race for taskId={} windowStart={}",
                        task.getId(), occurrence.windowStart());
                return existing;
            }
            return saveRunWithRunningStatus(task, schedule, occurrence, existing);
        }
    }

    private MaintenanceWindowRun saveRunWithRunningStatus(
            MaintenanceWindowTask task,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            MaintenanceWindowRun run) {
        long now = clock.getCurrentTimeMillis();
        if (run.getStatus() != null) {
            validatePriorRun(run, task, occurrence);
            run.setAttemptCount(nextAttemptCount(run));
        }
        run.setMaintenanceWindowSchedule(schedule);
        run.setStatus(MaintenanceRunStatus.RUNNING);
        run.setUpdatedAt(now);
        if (run.getWindowExecutionStart() == null) {
            run.setWindowExecutionStart(now);
        }
        run.setWindowExecutionEnd(null);
        run.setErrorDetail(null);
        run.setSkipReason(null);
        MaintenanceWindowRun saved = runRepository.saveAndFlush(run);
        LOGGER.debug("Marked maintenance run status RUNNING: taskId={} runId={} attemptCount={}",
                task.getId(), saved.getId(), saved.getAttemptCount());
        return saved;
    }

    private MaintenanceWindowRun newMaintenanceWindowRun(
            MaintenanceWindowTask task,
            WindowOccurrence occurrence,
            String policyRevision) {
        long now = clock.getCurrentTimeMillis();
        MaintenanceWindowRun run = new MaintenanceWindowRun();
        run.setMaintenanceWindowTask(task);
        run.setAccountId(task.getAccountId());
        run.setResourceCrn(task.getResourceCrn());
        run.setWindowStart(occurrence.windowStart());
        run.setWindowEnd(occurrence.windowEnd());
        run.setPolicyRevision(policyRevision);
        run.setCreatedAt(now);
        run.setAttemptCount(1);
        return run;
    }

    private MaintenanceWindowRun createSkippedRunIfAbsent(
            MaintenanceWindowTask task,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision,
            TaskDispatchSkipReason skipReason) {
        try {
            MaintenanceWindowRun saved = createTerminalRun(
                    task, schedule, occurrence, policyRevision, MaintenanceRunStatus.SKIPPED, null, skipReason);
            logSkippedRunWritten(task, occurrence, saved, skipReason);
            return saved;
        } catch (DataIntegrityViolationException e) {
            MaintenanceWindowRun existing = runRepository.findByMaintenanceWindowTaskIdAndWindowStart(
                            task.getId(), occurrence.windowStart())
                    .orElseThrow(() -> e);
            logSkippedRunNotWritten(task, occurrence, existing, skipReason);
            return existing;
        }
    }

    private void logSkippedRunWritten(
            MaintenanceWindowTask task,
            WindowOccurrence occurrence,
            MaintenanceWindowRun run,
            TaskDispatchSkipReason skipReason) {
        if (skipReason == null) {
            LOGGER.info("Recorded SKIPPED maintenance run for taskId={} runId={} windowStart={}",
                    task.getId(), run.getId(), occurrence.windowStart());
        } else {
            LOGGER.info("Recorded SKIPPED maintenance run for taskId={} runId={} windowStart={} reason={}",
                    task.getId(), run.getId(), occurrence.windowStart(), skipReason);
        }
    }

    private void logSkippedRunNotWritten(
            MaintenanceWindowTask task,
            WindowOccurrence occurrence,
            MaintenanceWindowRun existing,
            TaskDispatchSkipReason skipReason) {
        if (skipReason == null) {
            LOGGER.info(
                    "SKIPPED maintenance run not written for taskId={} windowStart={}: existing runId={} status={}",
                    task.getId(), occurrence.windowStart(), existing.getId(), existing.getStatus());
        } else {
            LOGGER.info(
                    "SKIPPED maintenance run not written for taskId={} windowStart={} reason={}: existing runId={} status={}",
                    task.getId(), occurrence.windowStart(), skipReason, existing.getId(), existing.getStatus());
        }
    }

    private static void validateRunBelongsToTask(MaintenanceWindowRun run, MaintenanceWindowTask task) {
        MaintenanceWindowTask runTask = run.getMaintenanceWindowTask();
        if (runTask == null || !task.getId().equals(runTask.getId())) {
            throw new IllegalArgumentException("run does not belong to task");
        }
    }

    private int nextAttemptCount(MaintenanceWindowRun priorRun) {
        return switch (priorRun.getStatus()) {
            case FAILED -> priorRun.getAttemptCount() + 1;
            case PLANNED -> priorRun.getAttemptCount();
            default -> throw new IllegalArgumentException(
                    "priorRun in status " + priorRun.getStatus() + " cannot transition to RUNNING");
        };
    }

    private static void validatePriorRun(
            MaintenanceWindowRun priorRun,
            MaintenanceWindowTask task,
            WindowOccurrence occurrence) {
        validateRunBelongsToTask(priorRun, task);
        if (priorRun.getWindowStart() != occurrence.windowStart()) {
            throw new IllegalArgumentException("priorRun does not belong to occurrence");
        }
    }

    private MaintenanceWindowRun createTerminalRun(
            MaintenanceWindowTask task,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision,
            MaintenanceRunStatus status,
            String errorDetail,
            TaskDispatchSkipReason skipReason) {
        long now = clock.getCurrentTimeMillis();
        MaintenanceWindowRun run = new MaintenanceWindowRun();
        run.setMaintenanceWindowTask(task);
        run.setAccountId(task.getAccountId());
        run.setResourceCrn(task.getResourceCrn());
        run.setMaintenanceWindowSchedule(schedule);
        run.setWindowStart(occurrence.windowStart());
        run.setWindowEnd(occurrence.windowEnd());
        run.setStatus(status);
        run.setPolicyRevision(policyRevision);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        run.setWindowExecutionStart(now);
        run.setWindowExecutionEnd(now);
        run.setErrorDetail(errorDetail);
        run.setSkipReason(skipReason != null ? skipReason.name() : null);
        run.setAttemptCount(1);
        return runRepository.saveAndFlush(run);
    }

    private void completeTaskIfOneShot(MaintenanceWindowTask task, long now) {
        if (task.getTaskKind() != MaintenanceTaskKind.ONE_SHOT) {
            return;
        }
        task.setStatus(MaintenanceTaskStatus.COMPLETED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        taskRepository.saveAndFlush(task);
    }

    private void completeOneShotIfTerminalFailure(MaintenanceWindowTask task, MaintenanceWindowRun run, long now) {
        if (task.getTaskKind() != MaintenanceTaskKind.ONE_SHOT) {
            return;
        }
        if (!task.isRetryWithinOccurrence()) {
            completeTaskIfOneShot(task, now);
            return;
        }
        if (run.getAttemptCount() >= task.getMaxAttemptsPerOccurrence()) {
            completeTaskIfOneShot(task, now);
        }
    }
}
