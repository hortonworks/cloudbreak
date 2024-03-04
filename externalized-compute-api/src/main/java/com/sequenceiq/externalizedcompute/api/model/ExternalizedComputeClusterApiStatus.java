package com.sequenceiq.externalizedcompute.api.model;

import java.util.EnumSet;
import java.util.Set;

public enum ExternalizedComputeClusterApiStatus {

    CREATE_IN_PROGRESS,
    DELETE_IN_PROGRESS,
    DELETED,
    AVAILABLE,
    CREATE_FAILED,
    DELETE_FAILED;

    private static final Set<ExternalizedComputeClusterApiStatus> FAILED_STATUSES = EnumSet.of(CREATE_FAILED, DELETE_FAILED);

    public boolean isFailed() {
        return FAILED_STATUSES.contains(this);
    }
}
