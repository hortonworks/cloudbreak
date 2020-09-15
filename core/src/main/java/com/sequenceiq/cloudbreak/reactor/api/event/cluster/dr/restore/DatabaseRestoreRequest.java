package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseRestoreRequest extends BackupRestoreEvent {

    public DatabaseRestoreRequest(Long stackId, String backupLocation, String backupId, String userCrn) {
        super(stackId, backupLocation, backupId, userCrn);
    }
}
