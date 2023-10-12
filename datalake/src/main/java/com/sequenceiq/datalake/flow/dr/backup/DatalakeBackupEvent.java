package com.sequenceiq.datalake.flow.dr.backup;

import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupCancelledEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupSuccessEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupCouldNotStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeFullBackupInProgressEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeBackupEvent implements FlowEvent {

    DATALAKE_DATABASE_BACKUP_EVENT(DatalakeDatabaseBackupStartEvent.class),
    DATALAKE_TRIGGER_BACKUP_EVENT(),
    DATALAKE_BACKUP_SERVICES_STOPPED_EVENT(),
    DATALAKE_DATABASE_BACKUP_COULD_NOT_START_EVENT(DatalakeDatabaseBackupCouldNotStartEvent.class),
    DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT(),
    DATALAKE_FULL_BACKUP_IN_PROGRESS_EVENT(DatalakeFullBackupInProgressEvent.class),
    DATALAKE_BACKUP_SUCCESS_EVENT(DatalakeBackupSuccessEvent.class),
    DATALAKE_BACKUP_CANCELLED_EVENT(DatalakeBackupCancelledEvent.class),
    DATALAKE_DATABASE_BACKUP_FAILED_EVENT(DatalakeDatabaseBackupFailedEvent.class),
    DATALAKE_BACKUP_FAILED_EVENT(DatalakeBackupFailedEvent.class),
    DATALAKE_BACKUP_FINALIZED_EVENT(),
    DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT(),
    DATALAKE_BACKUP_CANCEL_HANDLED_EVENT(),
    DATALAKE_BACKUP_FAILURE_HANDLED_EVENT();

    private final String event;

    DatalakeBackupEvent() {
        event = name();
    }

    DatalakeBackupEvent(Class eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}