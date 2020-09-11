package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class FullBackupStatusRequest extends BackupRestoreEvent {

    public FullBackupStatusRequest(Long stackId, String backupId, String userCrn) {
        super(stackId, null, backupId, userCrn);
    }
}
