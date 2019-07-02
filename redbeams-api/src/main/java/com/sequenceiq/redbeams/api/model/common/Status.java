package com.sequenceiq.redbeams.api.model.common;

import java.util.Arrays;
import java.util.List;

public enum Status {
    REQUESTED,
    CREATE_IN_PROGRESS,
    AVAILABLE,
    UPDATE_IN_PROGRESS,
    UPDATE_REQUESTED,
    UPDATE_FAILED,
    CREATE_FAILED,
    ENABLE_SECURITY_FAILED,
    DELETE_REQUESTED,
    PRE_DELETE_IN_PROGRESS,
    DELETE_IN_PROGRESS,
    DELETE_FAILED,
    DELETE_COMPLETED,
    STOPPED,
    STOP_REQUESTED,
    START_REQUESTED,
    STOP_IN_PROGRESS,
    START_IN_PROGRESS,
    START_FAILED,
    STOP_FAILED,
    WAIT_FOR_SYNC,
    MAINTENANCE_MODE_ENABLED;

    private static final List<Status> IS_REMOVABLE_STATUS_LIST = Arrays.asList(AVAILABLE, UPDATE_FAILED, CREATE_FAILED,
            ENABLE_SECURITY_FAILED, DELETE_FAILED, DELETE_COMPLETED, STOPPED, START_FAILED, STOP_FAILED);
    private static final List<Status> IS_AVAILABLE_LIST = Arrays.asList(AVAILABLE, MAINTENANCE_MODE_ENABLED);

    public boolean isRemovableStatus() {
        return IS_REMOVABLE_STATUS_LIST.contains(valueOf(name()));
    }

    public boolean isAvailable() {
        return IS_AVAILABLE_LIST.contains(valueOf(name()));
    }

    public boolean isStopPhaseActive() {
        return name().contains("STOP");
    }
}
