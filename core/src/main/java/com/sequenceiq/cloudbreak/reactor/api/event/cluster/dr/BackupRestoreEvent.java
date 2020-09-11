package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class BackupRestoreEvent extends StackEvent {

    private final String backupLocation;

    private final String backupId;

    private final String userCrn;

    public BackupRestoreEvent(Long stackId, String backupLocation, String backupId, String userCrn) {
        this (null, stackId, backupLocation, backupId, userCrn);
    }

    public BackupRestoreEvent(String selector, Long stackId, String backupLocation, String backupId, String userCrn) {
        super(selector, stackId);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.userCrn = userCrn;
    }

    public BackupRestoreEvent(String selector, Long stackId, Promise<AcceptResult> accepted, String backupLocation,
            String backupId, String userCrn) {
        super(selector, stackId, accepted);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.userCrn = userCrn;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupId() {
        return backupId;
    }

    public String getUserCrn() {
        return userCrn;
    }
}
