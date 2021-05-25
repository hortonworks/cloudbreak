package com.sequenceiq.datalake.flow.dr.backup;

public enum DatalakeBackupFailureReason {
    BACKUP_ON_UPGRADE("Backup performed on upgrade"),
    BACKUP_ON_MIGRATION("Backup performed on migration"),
    USER_TRIGGERED("Backup performed by user");
    private final String message;

    DatalakeBackupFailureReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
