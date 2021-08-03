package com.sequenceiq.freeipa.flow.freeipa.backup.full;

import com.sequenceiq.flow.core.FlowEvent;

public enum FullBackupEvent implements FlowEvent {
    FULL_BACKUP_EVENT,
    FULL_BACKUP_SUCCESSFUL_EVENT,
    FULL_BACKUP_FINISHED_EVENT,
    FULL_BACKUP_FAILED_EVENT,
    FULL_BACKUP_FAILURE_HANDLED_EVENT;

    private final String event;

    FullBackupEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
