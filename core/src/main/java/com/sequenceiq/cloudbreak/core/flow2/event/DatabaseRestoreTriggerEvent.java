package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

import reactor.rx.Promise;

public class DatabaseRestoreTriggerEvent extends BackupRestoreEvent {

    public DatabaseRestoreTriggerEvent(String selector, Long stackId, String backupLocation, String backupId) {
        super(selector, stackId, backupLocation, backupId, null);
    }

    public DatabaseRestoreTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted,
            String backupLocation, String backupId) {
        super(event, resourceId, accepted, backupLocation, backupId, null);
    }
}
