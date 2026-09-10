package com.sequenceiq.maintenance.dispatcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchEvaluation;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchEvaluationRequest;
import com.sequenceiq.maintenance.dispatcher.model.TaskDispatchSkipReason;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.repository.MaintenanceWindowRunRepository;
import com.sequenceiq.maintenance.repository.MaintenanceWindowTaskRepository;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;
import com.sequenceiq.maintenance.util.MaintenanceTaskResourceScope;

/**
 * Evaluates whether an ACTIVE task should dispatch for the current window occurrence.
 * <p>
 * Checks, in order: window still open; deduplication and retry policy on an existing run; explicit
 * {@code depends_on}; then implicit tier ordering (FreeIPA before Datalake before Datahub) when
 * {@code depends_on} is unset. Tasks on the same tier or {@code resourceCrn} are not serialized —
 * callers must set {@code depends_on} for hard prerequisites. {@link MaintenanceWindowTask#getPriority()
 * Priority} is applied by the dispatcher when iterating {@code activeTasks}, not inside this class.
 * <p>
 * <b>Retry attempts:</b> For a {@link MaintenanceRunStatus#FAILED} prior run, redispatch is allowed only while
 * {@link MaintenanceWindowRun#getAttemptCount()} is strictly less than
 * {@link MaintenanceWindowTask#getMaxAttemptsPerOccurrence()}. This class reads {@code attemptCount} but never
 * writes it — the dispatcher must increment {@code attempt_count} on the run row when each new attempt begins
 * (initial dispatch and each retry). If the dispatcher dispatches without incrementing, retries are not capped.
 * <p>
 * <b>Window bounds:</b> Only {@code now >= windowEnd} is rejected ({@link TaskDispatchSkipReason#WINDOW_ENDED}).
 * The caller must not invoke {@link #evaluate} before {@code windowStart} — schedule eligibility is responsible
 * for confirming an active occurrence; this class does not guard {@code now < windowStart}.
 * <p>
 * <b>Dependencies:</b> Prerequisite tasks are resolved from the tick snapshot ({@link TaskDispatchEvaluationRequest#activeTasks()})
 * when {@link com.sequenceiq.maintenance.domain.MaintenanceTaskStatus#ACTIVE ACTIVE}; when absent, loaded via
 * {@link MaintenanceWindowTaskRepository#findByIdAndStatusIn(Long, java.util.Collection)} ({@link MaintenanceTaskStatus#ACTIVE}
 * or {@link MaintenanceTaskStatus#COMPLETED}) to support completed
 * {@link com.sequenceiq.maintenance.domain.MaintenanceTaskKind#ONE_SHOT ONE_SHOT} prerequisites. Prerequisite completion
 * is determined by the most recent run
 * whose {@code [windowStart, windowEnd)} overlaps the
 * dependent occurrence (half-open interval). Overlap on persisted run rows is used instead of schedule expansion or
 * a fixed wall-clock lookback so prerequisites on a different cadence (e.g. weekly vs daily) or with misaligned window
 * boundaries are still matched — a lookback can miss a completed run that started before the cutoff but overlaps the
 * dependent window.
 * <p>
 * <b>Implicit ordering:</b> When {@code depends_on} is unset, {@link #implicitOrderingBlockReason} walks platform
 * tiers in order (FreeIPA, then Datalake, then Datahub) and may invoke {@link #prerequisiteBlockReason} for each
 * same-environment peer on a lower tier, each incurring one overlap query on {@link MaintenanceWindowRunRepository}.
 */
@Component
public class MaintenanceWindowTaskDispatchEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowTaskDispatchEvaluator.class);

    private static final int LOWEST_PLATFORM_TIER = MaintenanceScopeType.FREEIPA.implicitPlatformOrder();

    private static final Set<MaintenanceTaskStatus> DEPENDENCY_RESOLVABLE_STATUSES = Set.of(
            MaintenanceTaskStatus.ACTIVE,
            MaintenanceTaskStatus.COMPLETED);

    private final MaintenanceWindowRunRepository runRepository;

    private final MaintenanceWindowTaskRepository taskRepository;

    private final Clock clock;

    private final MaintenanceTaskResourceScope resourceScope;

    public MaintenanceWindowTaskDispatchEvaluator(
            MaintenanceWindowRunRepository runRepository,
            MaintenanceWindowTaskRepository taskRepository,
            Clock clock,
            MaintenanceTaskResourceScope resourceScope) {
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
        this.resourceScope = resourceScope;
    }

    public TaskDispatchEvaluation evaluate(TaskDispatchEvaluationRequest request) {
        MaintenanceWindowTask task = request.task();
        WindowOccurrence occurrence = request.occurrence();
        Instant now = clock.getCurrentInstant();
        MaintenanceWindowRun priorRun = existingRun(task, occurrence).orElse(null);
        if (!now.isBefore(Instant.ofEpochMilli(occurrence.windowEnd()))) {
            return skip(task, occurrence, priorRun, TaskDispatchSkipReason.WINDOW_ENDED);
        }
        Optional<TaskDispatchSkipReason> dedupReason = dedupSkipReason(task, priorRun, now);
        if (dedupReason.isPresent()) {
            return skip(task, occurrence, priorRun, dedupReason.get());
        }
        if (task.getDependsOnTaskId() != null) {
            Optional<TaskDispatchSkipReason> dependencyReason = dependencySkipReason(task, occurrence, request.activeTasks(), now);
            if (dependencyReason.isPresent()) {
                return skip(task, occurrence, priorRun, dependencyReason.get());
            }
        } else {
            Optional<TaskDispatchSkipReason> implicitReason = implicitOrderingBlockReason(task, occurrence, request.activeTasks());
            if (implicitReason.isPresent()) {
                return skip(task, occurrence, priorRun, implicitReason.get());
            }
        }
        LOGGER.info("Dispatching maintenance task: taskId={} windowStart={}", task.getId(), occurrence.windowStart());
        return TaskDispatchEvaluation.dispatch(occurrence, priorRun);
    }

    private TaskDispatchEvaluation skip(
            MaintenanceWindowTask task,
            WindowOccurrence occurrence,
            MaintenanceWindowRun priorRun,
            TaskDispatchSkipReason reason) {
        LOGGER.debug("Skipping maintenance task dispatch: taskId={} windowStart={} reason={}",
                task.getId(), occurrence.windowStart(), reason);
        return TaskDispatchEvaluation.skip(reason, occurrence, priorRun);
    }

    private Optional<MaintenanceWindowRun> existingRun(MaintenanceWindowTask task, WindowOccurrence occurrence) {
        return runRepository.findByMaintenanceWindowTaskIdAndWindowStart(task.getId(), occurrence.windowStart());
    }

    private Optional<TaskDispatchSkipReason> dedupSkipReason(
            MaintenanceWindowTask task,
            MaintenanceWindowRun priorRun,
            Instant now) {
        if (priorRun == null) {
            return Optional.empty();
        }
        return switch (priorRun.getStatus()) {
            case RUNNING -> Optional.of(TaskDispatchSkipReason.DEDUP_RUNNING);
            case COMPLETED -> Optional.of(TaskDispatchSkipReason.DEDUP_COMPLETED);
            case SKIPPED -> Optional.of(TaskDispatchSkipReason.DEDUP_SKIPPED);
            case FAILED -> failedRetrySkipReason(task, priorRun, now);
            case PLANNED -> Optional.empty();
        };
    }

    /**
     * Compares {@link MaintenanceWindowRun#getAttemptCount()} against {@link MaintenanceWindowTask#getMaxAttemptsPerOccurrence()}
     * and cooldown; does not modify the run row.
     */
    private Optional<TaskDispatchSkipReason> failedRetrySkipReason(
            MaintenanceWindowTask task,
            MaintenanceWindowRun run,
            Instant now) {
        if (retryPolicyTerminated(task, run)) {
            return Optional.of(TaskDispatchSkipReason.RETRY_NOT_ALLOWED);
        }
        if (task.getRetryCooldownMinutes() > 0) {
            if (run.getWindowExecutionEnd() == null) {
                return Optional.of(TaskDispatchSkipReason.RETRY_COOLDOWN);
            }
            Instant cooldownEnd = Instant.ofEpochMilli(run.getWindowExecutionEnd())
                    .plus(task.getRetryCooldownMinutes(), ChronoUnit.MINUTES);
            if (now.isBefore(cooldownEnd)) {
                return Optional.of(TaskDispatchSkipReason.RETRY_COOLDOWN);
            }
        }
        return Optional.empty();
    }

    /**
     * True when a {@link MaintenanceRunStatus#FAILED} run cannot be retried within this occurrence:
     * retries are disabled or {@link MaintenanceWindowRun#getAttemptCount()} has reached
     * {@link MaintenanceWindowTask#getMaxAttemptsPerOccurrence()}. Cooldown and other dispatch gates are
     * intentionally excluded — callers that need "may redispatch now?" should use
     * {@link #failedRetrySkipReason}.
     */
    private boolean retryPolicyTerminated(MaintenanceWindowTask task, MaintenanceWindowRun run) {
        if (!task.isRetryWithinOccurrence()) {
            return true;
        }
        return run.getAttemptCount() >= task.getMaxAttemptsPerOccurrence();
    }

    private Optional<TaskDispatchSkipReason> dependencySkipReason(
            MaintenanceWindowTask task,
            WindowOccurrence occurrence,
            List<MaintenanceWindowTask> activeTasks,
            Instant now) {
        Long dependencyTaskId = task.getDependsOnTaskId();
        MaintenanceWindowTask dependency = resolveDependencyTask(dependencyTaskId, activeTasks);
        if (dependency == null) {
            return Optional.of(TaskDispatchSkipReason.DEPENDENCY_NOT_FOUND);
        }
        if (!task.getAccountId().equals(dependency.getAccountId())) {
            return Optional.of(TaskDispatchSkipReason.DEPENDENCY_SCOPE_MISMATCH);
        }
        if (!task.getEnvironmentCrn().equals(dependency.getEnvironmentCrn())) {
            return Optional.of(TaskDispatchSkipReason.DEPENDENCY_SCOPE_MISMATCH);
        }
        return prerequisiteBlockReason(dependency, occurrence);
    }

    /**
     * Resolves {@code depends_on_task_id} from the tick's {@code activeTasks} snapshot, falling back to
     * {@link MaintenanceWindowTaskRepository#findByIdAndStatusIn(Long, java.util.Collection)} when the prerequisite is
     * absent (e.g. a completed {@link com.sequenceiq.maintenance.domain.MaintenanceTaskKind#ONE_SHOT ONE_SHOT} no longer
     * in {@link MaintenanceTaskStatus#ACTIVE}). Soft-deleted ({@link MaintenanceTaskStatus#DELETED}) and
     * {@link MaintenanceTaskStatus#DISABLED} rows are excluded.
     */
    private MaintenanceWindowTask resolveDependencyTask(Long taskId, List<MaintenanceWindowTask> activeTasks) {
        MaintenanceWindowTask dependency = findActiveTask(taskId, activeTasks);
        if (dependency != null) {
            return dependency;
        }
        return taskRepository.findByIdAndStatusIn(taskId, DEPENDENCY_RESOLVABLE_STATUSES).orElse(null);
    }

    private MaintenanceWindowTask findActiveTask(Long taskId, List<MaintenanceWindowTask> activeTasks) {
        for (MaintenanceWindowTask candidate : activeTasks) {
            if (taskId.equals(candidate.getId())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Empty when the prerequisite has a {@link MaintenanceRunStatus#COMPLETED} run overlapping the dependent occurrence;
     * otherwise returns a skip reason distinguishing still-waiting from terminal prerequisite failure/skip.
     */
    private Optional<TaskDispatchSkipReason> prerequisiteBlockReason(
            MaintenanceWindowTask prerequisite,
            WindowOccurrence dependentOccurrence) {
        Optional<MaintenanceWindowRun> run = findOverlappingPrerequisiteRun(prerequisite.getId(), dependentOccurrence);
        if (run.isEmpty()) {
            return Optional.of(TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED);
        }
        return switch (run.get().getStatus()) {
            case COMPLETED -> Optional.empty();
            case SKIPPED -> Optional.of(TaskDispatchSkipReason.DEPENDENCY_SKIPPED);
            case FAILED -> prerequisiteTerminallyFailed(prerequisite, run.get())
                    ? Optional.of(TaskDispatchSkipReason.DEPENDENCY_FAILED)
                    : Optional.of(TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED);
            case RUNNING, PLANNED -> Optional.of(TaskDispatchSkipReason.DEPENDENCY_NOT_COMPLETED);
        };
    }

    /**
     * Loads the overlapping prerequisite run for dependency / implicit-ordering checks.
     * See {@link MaintenanceWindowRunRepository
     * #findFirstByMaintenanceWindowTaskIdAndWindowStartLessThanAndWindowEndGreaterThanOrderByWindowStartDesc}
     * for interval and {@code windowStart DESC} tie-break semantics.
     */
    private Optional<MaintenanceWindowRun> findOverlappingPrerequisiteRun(Long prerequisiteTaskId, WindowOccurrence dependentOccurrence) {
        return runRepository.findFirstByMaintenanceWindowTaskIdAndWindowStartLessThanAndWindowEndGreaterThanOrderByWindowStartDesc(
                prerequisiteTaskId, dependentOccurrence.windowEnd(), dependentOccurrence.windowStart());
    }

    /**
     * True when a FAILED prerequisite will not produce another attempt within the overlapping occurrence.
     * Uses {@link #retryPolicyTerminated} only — prerequisite cooldown does not make the dependency terminal.
     */
    private boolean prerequisiteTerminallyFailed(MaintenanceWindowTask prerequisite, MaintenanceWindowRun run) {
        return retryPolicyTerminated(prerequisite, run);
    }

    /**
     * Blocks until lower platform tiers complete: FreeIPA, then Datalake, then Datahub. Peers on the same tier are
     * not serialized here. Returns empty when {@code environmentCrn} is blank or the task is already on the lowest tier.
     */
    private Optional<TaskDispatchSkipReason> implicitOrderingBlockReason(
            MaintenanceWindowTask task,
            WindowOccurrence occurrence,
            List<MaintenanceWindowTask> activeTasks) {
        String environmentCrn = task.getEnvironmentCrn();
        if (environmentCrn == null || environmentCrn.isBlank()) {
            return Optional.empty();
        }
        int taskTier = implicitPlatformOrder(task.getResourceCrn());
        if (taskTier <= LOWEST_PLATFORM_TIER) {
            return Optional.empty();
        }
        // Datahub (tier 2): check FreeIPA (tier 0), then Datalake (tier 1)
        // Datalake (tier 1): check FreeIPA only
        // FreeIPA (tier 0): no implicit check
        for (int tier = LOWEST_PLATFORM_TIER; tier < taskTier; tier++) {
            Optional<TaskDispatchSkipReason> blockReason = blockReasonFromPeersAtTier(
                    task, occurrence, activeTasks, environmentCrn, tier);
            if (blockReason.isPresent()) {
                return blockReason;
            }
        }
        return Optional.empty();
    }

    private Optional<TaskDispatchSkipReason> blockReasonFromPeersAtTier(
            MaintenanceWindowTask task,
            WindowOccurrence occurrence,
            List<MaintenanceWindowTask> activeTasks,
            String environmentCrn,
            int tier) {
        for (MaintenanceWindowTask candidate : activeTasks) {
            if (!Objects.equals(environmentCrn, candidate.getEnvironmentCrn())) {
                continue;
            }
            if (candidate.getId().equals(task.getId()) || candidate.getDependsOnTaskId() != null) {
                continue;
            }
            if (implicitPlatformOrder(candidate.getResourceCrn()) != tier) {
                continue;
            }
            Optional<TaskDispatchSkipReason> blockReason = prerequisiteBlockReason(candidate, occurrence)
                    .map(this::mapImplicitPrerequisiteBlockReason);
            if (blockReason.isPresent()) {
                return blockReason;
            }
        }
        return Optional.empty();
    }

    private int implicitPlatformOrder(String resourceCrn) {
        return resourceScope.scopeTypeFromResourceCrn(resourceCrn).implicitPlatformOrder();
    }

    private TaskDispatchSkipReason mapImplicitPrerequisiteBlockReason(TaskDispatchSkipReason prerequisiteReason) {
        return switch (prerequisiteReason) {
            case DEPENDENCY_FAILED -> TaskDispatchSkipReason.IMPLICIT_PREREQUISITE_FAILED;
            case DEPENDENCY_SKIPPED -> TaskDispatchSkipReason.IMPLICIT_PREREQUISITE_SKIPPED;
            case DEPENDENCY_NOT_COMPLETED -> TaskDispatchSkipReason.IMPLICIT_PLATFORM_ORDERING;
            case WINDOW_ENDED, DEPENDENCY_NOT_FOUND, DEPENDENCY_SCOPE_MISMATCH, IMPLICIT_PLATFORM_ORDERING,
                    IMPLICIT_PREREQUISITE_FAILED, IMPLICIT_PREREQUISITE_SKIPPED, DEDUP_RUNNING, DEDUP_COMPLETED,
                    DEDUP_SKIPPED, RETRY_NOT_ALLOWED, RETRY_COOLDOWN ->
                    throw new IllegalStateException("Unexpected prerequisite reason for implicit ordering: " + prerequisiteReason);
        };
    }
}
