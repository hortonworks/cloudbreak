package com.sequenceiq.datalake.flow.dr.restore;

public enum DatalakeRestoreReason {
    RESTORE_ON_UPGRADE_FAILURE("Restore performed on upgrade"),
    RESTORE_ON_MIGRATION("Restore performed on migration"),
    USER_TRIGGERED("Restore performed by user");
    private final String message;

    DatalakeRestoreReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
