package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatalakeBackupSuccess extends BackupRestoreEvent {

    public DatalakeBackupSuccess(Long stackId) {
        super(stackId, null, null, null);
    }
}
