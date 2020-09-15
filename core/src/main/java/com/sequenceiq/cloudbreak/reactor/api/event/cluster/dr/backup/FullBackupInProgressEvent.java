package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class FullBackupInProgressEvent extends BackupRestoreEvent {

    public FullBackupInProgressEvent(Long stackId, String backupId, String userCrn) {
        super(stackId, null, backupId, userCrn);
    }
}
