package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatabaseBackupEvent implements FlowEvent {
    DATABASE_BACKUP_EVENT("DATABASE_BACKUP_EVENT"),
    DATABASE_BACKUP_FINISHED_EVENT(EventSelectorUtil.selector(DatabaseBackupSuccess.class)),
    DATABASE_BACKUP_FAILED_EVENT(EventSelectorUtil.selector(DatabaseBackupFailedEvent.class)),
    DATABASE_BACKUP_FINALIZED_EVENT("DATABASE_BACKUP_FINALIZED_EVENT"),
    DATABASE_BACKUP_FAIL_HANDLED_EVENT("DATABASE_BACKUP_FAIL_HANDLED_EVENT");

    private final String event;

    DatabaseBackupEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
