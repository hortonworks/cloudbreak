package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseBackupSuccess extends BackupRestoreEvent {

    public DatabaseBackupSuccess(Long stackId) {
        super(stackId, null, null);
    }
}
