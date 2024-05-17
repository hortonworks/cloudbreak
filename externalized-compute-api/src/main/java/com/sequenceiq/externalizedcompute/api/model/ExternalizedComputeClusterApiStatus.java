package com.sequenceiq.externalizedcompute.api.model;

import java.util.EnumSet;
import java.util.Set;

public enum ExternalizedComputeClusterApiStatus {

    CREATE_IN_PROGRESS,
    REINITIALIZE_IN_PROGRESS,
    DELETE_IN_PROGRESS,
    DELETED,
    AVAILABLE,
    CREATE_FAILED,
    DELETE_FAILED,
    UNKNOWN;

    private static final Set<ExternalizedComputeClusterApiStatus> FAILED_STATUSES = EnumSet.of(CREATE_FAILED, DELETE_FAILED);

    public boolean isFailed() {
        return FAILED_STATUSES.contains(this);
    }

    public boolean isCreationInProgress() {
        return CREATE_IN_PROGRESS.equals(this) || REINITIALIZE_IN_PROGRESS.equals(this);
    }

    public boolean isDeletionInProgress() {
        return DELETE_IN_PROGRESS.equals(this);
    }

    public boolean isDeleted() {
        return DELETED.equals(this);
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(this);
    }
}
