package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatabaseRestoreSuccess extends BackupRestoreEvent {

    public DatabaseRestoreSuccess(Long stackId) {
        super(stackId, null, null);
    }
}
