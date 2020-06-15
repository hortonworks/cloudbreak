package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseBackupRequest extends BackupRestoreEvent {

    public DatabaseBackupRequest(Long stackId, String backupLocation, String backupId) {
        super(stackId, backupLocation, backupId);
    }
}
