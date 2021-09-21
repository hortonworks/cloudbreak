package com.sequenceiq.datalake.flow.dr.restore;

public enum DatalakeRestoreFailureReason {
    RESTORE_ON_UPGRADE_FAILURE("Restore performed on upgrade"),
    RESTORE_ON_RESIZE("Restore performed on resize"),
    USER_TRIGGERED("Restore performed by user");
    private final String message;

    DatalakeRestoreFailureReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
