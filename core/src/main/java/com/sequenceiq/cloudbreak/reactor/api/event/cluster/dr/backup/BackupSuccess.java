package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class BackupSuccess extends BackupRestoreEvent {

    public BackupSuccess(Long stackId) {
        super(stackId, null, null, null);
    }
}
