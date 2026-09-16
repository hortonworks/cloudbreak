package com.sequenceiq.maintenance.dispatcher;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterDispatchResult;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchEvaluation;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchEvaluationRequest;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchSkipReason;
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

/**
 * Orchestrates one maintenance window dispatcher tick: load ACTIVE tasks, resolve schedule eligibility,
 * evaluate dispatch gates, persist run outcomes, and invoke submitter execute callbacks.
 * <p>
 * <b>Durability caveat.</b> {@link MaintenanceWindowRunService#markRunning} commits its own transaction, then the
 * submitter execute callback is invoked outside any transaction (blocking HTTP with a long read timeout), then
 * {@link MaintenanceWindowRunService#applySubmitterOutcome} commits again. If the process is killed between the two
 * commits, the run row is left in {@code RUNNING}; subsequent ticks hit the evaluator's
 * {@link TaskDispatchSkipReason#DEDUP_RUNNING} gate (non-terminal defer), so {@code markRunning} is never reached.
 * A separate reconciler is required to age out orphan
 * {@code RUNNING} rows; TODO(CB-33852) wire that in before Phase 2 GA.
 */
@Service
public class MaintenanceWindowDispatchTickService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowDispatchTickService.class);

    private final MaintenanceWindowTaskRepository taskRepository;

    private final MaintenanceWindowScheduleEligibilityService scheduleEligibilityService;

    private final MaintenanceWindowTaskDispatchEvaluator dispatchEvaluator;

    private final MaintenanceWindowRunService runService;

    private final MaintenanceTaskSubmitterClient submitterClient;

    private final MaintenanceTaskResourceScope resourceScope;

    private final Clock clock;

    @Inject
    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    public MaintenanceWindowDispatchTickService(
            MaintenanceWindowTaskRepository taskRepository,
            MaintenanceWindowScheduleEligibilityService scheduleEligibilityService,
            MaintenanceWindowTaskDispatchEvaluator dispatchEvaluator,
            MaintenanceWindowRunService runService,
            MaintenanceTaskSubmitterClient submitterClient,
            MaintenanceTaskResourceScope resourceScope,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.scheduleEligibilityService = scheduleEligibilityService;
        this.dispatchEvaluator = dispatchEvaluator;
        this.runService = runService;
        this.submitterClient = submitterClient;
        this.resourceScope = resourceScope;
        this.clock = clock;
    }

    /**
     * Entry point for one dispatcher cycle, invoked by the Quartz job on {@code maintenance.dispatcher.interval-minutes}.
     * <p>
     * Loads all ACTIVE tasks (priority desc, then registration time asc) into a snapshot, then for each task:
     * {@link MaintenanceWindowScheduleEligibilityService#checkEligibility} →
     * {@link MaintenanceWindowTaskDispatchEvaluator#evaluate} → on approval
     * {@link MaintenanceWindowRunService#markRunning} →
     * {@link MaintenanceTaskSubmitterClient#invokeExecuteCallback} →
     * {@link MaintenanceWindowRunService#applySubmitterOutcome}. Non-dispatchable or evaluator-deferred tasks may
     * record a terminal {@code SKIPPED} run when the skip reason will not resolve within the window.
     * <p>
     * Per-task failures are caught and logged so one bad task does not block the rest of the tick. Schedule
     * eligibility uses a single {@code now} at tick start for every task; {@link MaintenanceWindowTaskDispatchEvaluator}
     * reads wall clock on each call (e.g. {@link TaskDispatchSkipReason#WINDOW_ENDED}), so this is not one frozen
     * snapshot for the whole tick—usually sub-second skew.
     */
    public void tick() {
        long tickStartMs = clock.getCurrentTimeMillis();
        List<MaintenanceWindowTask> activeTasksSnapshot =
                taskRepository.findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus.ACTIVE);
        LOGGER.info("Maintenance window dispatcher tick started with {} ACTIVE task(s)", activeTasksSnapshot.size());
        for (MaintenanceWindowTask task : activeTasksSnapshot) {
            try {
                processTask(task, activeTasksSnapshot, tickStartMs);
            } catch (RuntimeException e) {
                LOGGER.error("Dispatcher tick failed for maintenance task id={}", task.getId(), e);
            }
        }
        LOGGER.info("Maintenance window dispatcher tick finished in {} ms", clock.getCurrentTimeMillis() - tickStartMs);
    }

    private void processTask(MaintenanceWindowTask task, List<MaintenanceWindowTask> activeTasksSnapshot, long nowMs) {
        MaintenanceWindowResourceIdentity identity = toIdentity(task);
        MaintenanceWindowScheduleEligibility eligibility = scheduleEligibilityService.checkEligibility(identity, nowMs);
        if (!eligibility.dispatchable()) {
            handleNonDispatchableSchedule(task, eligibility);
            return;
        }
        MaintenanceWindowSchedule schedule = eligibility.schedule().orElseThrow();
        WindowOccurrence occurrence = eligibility.currentOccurrence().orElseThrow();
        String policyRevision = MaintenanceWindowRunService.policyRevision(schedule);

        TaskDispatchEvaluation evaluation = dispatchEvaluator.evaluate(
                new TaskDispatchEvaluationRequest(task, occurrence, activeTasksSnapshot));
        if (!evaluation.shouldDispatch()) {
            handleEvaluatorSkip(task, schedule, occurrence, policyRevision, evaluation);
            return;
        }

        MaintenanceWindowRun run = runService.markRunning(
                task, schedule, occurrence, policyRevision, evaluation.priorRun());
        MaintenanceTaskSubmitterDispatchResult executeResult = submitterClient.invokeExecuteCallback(
                task, run, schedule, occurrence, policyRevision);
        runService.applySubmitterOutcome(task, run, executeResult);
        LOGGER.info("Dispatched maintenance task id={} runId={} windowStart={} outcome={}",
                task.getId(), run.getId(), occurrence.windowStart(), executeResult.outcome());
    }

    private void handleNonDispatchableSchedule(MaintenanceWindowTask task, MaintenanceWindowScheduleEligibility eligibility) {
        if (eligibility.schedule().isEmpty()) {
            LOGGER.debug("Skipping maintenance task id={}: no applicable schedule configured", task.getId());
            return;
        }
        MaintenanceWindowSchedule schedule = eligibility.schedule().get();
        if (eligibility.currentOccurrence().isEmpty()) {
            LOGGER.debug("Skipping maintenance task id={}: outside active window for schedule id={}",
                    task.getId(), schedule.getId());
            return;
        }
        WindowOccurrence occurrence = eligibility.currentOccurrence().get();
        String policyRevision = MaintenanceWindowRunService.policyRevision(schedule);
        runService.recordSkipped(
                task, schedule, occurrence, policyRevision, TaskDispatchSkipReason.SCHEDULE_OCCURRENCE_SKIPPED);
    }

    private void handleEvaluatorSkip(
            MaintenanceWindowTask task,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision,
            TaskDispatchEvaluation evaluation) {
        TaskDispatchSkipReason reason = evaluation.skipReason().orElseThrow();
        LOGGER.debug("Deferring maintenance task id={} windowStart={} reason={}",
                task.getId(), occurrence.windowStart(), reason);
        if (recordsTerminalSkippedRun(reason)) {
            runService.recordSkipped(task, schedule, occurrence, policyRevision, reason);
        }
    }

    /**
     * Whether to persist a terminal SKIPPED run for this occurrence when the evaluator defers dispatch.
     * <p>
     * {@code true}: without a run row, the occurrence would leave no audit trail and the skip reason will not
     * change before the window ends (e.g. dependency failed, window ended).
     * <p>
     * {@code false}: do not insert another SKIPPED row — either a run already exists for
     * {@code (task, windowStart)} (e.g. dedup / retry-not-allowed), or dispatch may still succeed on a later tick
     * in the same occurrence (e.g. dependency not yet completed, retry cooldown).
     * Package-private for parametrized testing.
     */
    static boolean recordsTerminalSkippedRun(TaskDispatchSkipReason reason) {
        return switch (reason) {
            case SCHEDULE_OCCURRENCE_SKIPPED, WINDOW_ENDED, DEPENDENCY_SKIPPED, DEPENDENCY_FAILED,
                    IMPLICIT_PREREQUISITE_SKIPPED, IMPLICIT_PREREQUISITE_FAILED -> true;
            case DEPENDENCY_NOT_FOUND, DEPENDENCY_SCOPE_MISMATCH, DEPENDENCY_NOT_COMPLETED,
                    IMPLICIT_PLATFORM_ORDERING, DEDUP_RUNNING, DEDUP_COMPLETED, DEDUP_SKIPPED, RETRY_NOT_ALLOWED,
                    RETRY_COOLDOWN -> false;
        };
    }

    private MaintenanceWindowResourceIdentity toIdentity(MaintenanceWindowTask task) {
        return new MaintenanceWindowResourceIdentity(
                task.getAccountId(),
                task.getEnvironmentCrn(),
                task.getResourceCrn(),
                resourceScope.scopeTypeFromResourceCrn(task.getResourceCrn()));
    }
}
