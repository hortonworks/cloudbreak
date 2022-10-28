package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database;

public enum DatabaseServerStatus {

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
    MAINTENANCE_MODE_ENABLED,
    UPGRADE_REQUESTED,
    UPGRADE_IN_PROGRESS,
    UPGRADE_FAILED,
    UNKNOWN;

    public boolean isAvailableForUpgrade() {
        return AVAILABLE.equals(this)
                || UPGRADE_FAILED.equals(this);
    }
}
