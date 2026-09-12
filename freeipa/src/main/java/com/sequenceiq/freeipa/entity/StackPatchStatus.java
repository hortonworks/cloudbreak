package com.sequenceiq.freeipa.entity;

import java.util.Set;

public enum StackPatchStatus {
    SCHEDULED,
    UNSCHEDULED,
    NOT_AFFECTED,
    AFFECTED,
    FIXED,
    FAILED,
    SKIPPED,
    UNKNOWN;

    private static final Set<StackPatchStatus> FINAL_STATUSES = Set.of(UNSCHEDULED, NOT_AFFECTED, FIXED);

    public boolean isFinal() {
        return FINAL_STATUSES.contains(this);
    }
}
