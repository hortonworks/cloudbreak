package com.sequenceiq.maintenance.dispatcher.model;

public enum TaskDispatchSkipReason {
    /** Customer-declared skip of this schedule occurrence ({@code maintenance_window_skip}). */
    SCHEDULE_OCCURRENCE_SKIPPED,
    WINDOW_ENDED,
    DEPENDENCY_NOT_FOUND,
    DEPENDENCY_SCOPE_MISMATCH,
    DEPENDENCY_NOT_COMPLETED,
    DEPENDENCY_FAILED,
    DEPENDENCY_SKIPPED,
    IMPLICIT_PLATFORM_ORDERING,
    IMPLICIT_PREREQUISITE_FAILED,
    IMPLICIT_PREREQUISITE_SKIPPED,
    DEDUP_RUNNING,
    DEDUP_COMPLETED,
    DEDUP_SKIPPED,
    RETRY_NOT_ALLOWED,
    RETRY_COOLDOWN
}
