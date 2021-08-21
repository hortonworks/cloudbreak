package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

import reactor.rx.Promise;

public class DatabaseBackupTriggerEvent extends BackupRestoreEvent {

    public DatabaseBackupTriggerEvent(String selector, Long stackId, String backupLocation, String backupId, boolean closeConnections) {
        super(selector, stackId, backupLocation, backupId, closeConnections);
    }

    public DatabaseBackupTriggerEvent(String selector, Long stackId, String backupLocation, String backupId) {
        super(selector, stackId, backupLocation, backupId);
    }

    public DatabaseBackupTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted,
            String backupLocation, String backupId, boolean closeConnections) {
        super(event, resourceId, accepted, backupLocation, backupId, closeConnections);
    }
}
