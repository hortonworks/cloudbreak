package com.sequenceiq.cloudbreak.cloud.model;

public enum BackupOperationType {
    ANY,
    BACKUP,
    RESTORE,
    NONE;

    public static boolean isRestore(BackupOperationType operationType) {
        return operationType == ANY || operationType == RESTORE;
    }
}
