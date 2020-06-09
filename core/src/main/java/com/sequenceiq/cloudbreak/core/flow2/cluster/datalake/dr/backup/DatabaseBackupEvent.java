package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatabaseBackupEvent implements FlowEvent {
    DATABASE_BACKUP_EVENT("DATABASE_BACKUP_EVENT"),
    DATABASE_BACKUP_FINISHED_EVENT("DATABASE_BACKUP_SUCCESS_EVENT"),
    DATABASE_BACKUP_FAILED_EVENT("DATABASE_BACKUP_FAILED_EVENT"),
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
