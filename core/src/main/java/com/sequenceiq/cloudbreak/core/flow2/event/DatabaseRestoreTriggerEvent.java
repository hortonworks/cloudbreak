package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;

import reactor.rx.Promise;

public class DatabaseRestoreTriggerEvent extends BackupRestoreEvent {

    public DatabaseRestoreTriggerEvent(String selector, Long stackId, String backupLocation, String backupId) {
        super(selector, stackId, backupLocation, backupId);
    }

    public DatabaseRestoreTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted,
            String backupLocation, String backupId) {
        super(event, resourceId, accepted, backupLocation, backupId);
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(DatabaseRestoreTriggerEvent.class, other,
                event -> Objects.equals(getBackupId(), event.getBackupId())
                        && Objects.equals(getBackupLocation(), event.getBackupLocation())
                        && getCloseConnections() == event.getCloseConnections());
    }
}
