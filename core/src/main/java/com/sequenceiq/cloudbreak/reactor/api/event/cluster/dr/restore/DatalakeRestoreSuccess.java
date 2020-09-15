package com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

public class DatalakeRestoreSuccess extends BackupRestoreEvent {

    public DatalakeRestoreSuccess(Long stackId) {
        super(stackId, null, null, null);
    }
}
