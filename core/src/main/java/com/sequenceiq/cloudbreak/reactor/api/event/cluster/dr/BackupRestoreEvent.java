package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class BackupRestoreEvent extends StackEvent {

    private final String backupLocation;

    private final String backupId;

    private final boolean closeConnections;

    public BackupRestoreEvent(Long stackId, String backupLocation, String backupId) {
        this (null, stackId, backupLocation, backupId);
    }

    public BackupRestoreEvent(Long stackId, String backupLocation, String backupId, boolean closeConnections) {
        this(null, stackId, backupLocation, backupId, closeConnections);
    }

    public BackupRestoreEvent(String selector, Long stackId, String backupLocation, String backupId) {
        super(selector, stackId);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = true;
    }

    public BackupRestoreEvent(String selector, Long stackId, String backupLocation, String backupId, boolean closeConnections) {
        super(selector, stackId);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = closeConnections;
    }

    public BackupRestoreEvent(String selector, Long stackId, Promise<AcceptResult> accepted, String backupLocation, String backupId) {
        super(selector, stackId, accepted);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = true;
    }

    public BackupRestoreEvent(String selector, Long stackId, Promise<AcceptResult> accepted, String backupLocation, String backupId, boolean closeConnections) {
        super(selector, stackId, accepted);
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.closeConnections = closeConnections;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupId() {
        return backupId;
    }

    public boolean getCloseConnections() {
        return closeConnections;
    }
}
