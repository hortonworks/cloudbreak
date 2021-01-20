package com.sequenceiq.datalake.flow.dr.backup;

import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupCouldNotStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupSuccessEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeFullBackupInProgressEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeBackupEvent implements FlowEvent {

    DATALAKE_DATABASE_BACKUP_EVENT(EventSelectorUtil.selector(DatalakeDatabaseBackupStartEvent.class)),
    DATALAKE_TRIGGER_BACKUP_EVENT("DATALAKE_TRIGGER_BACKUP_EVENT"),
    DATALAKE_DATABASE_BACKUP_COULD_NOT_START_EVENT(EventSelectorUtil.selector(DatalakeDatabaseBackupCouldNotStartEvent.class)),
    DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT("DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT"),
    DATALAKE_FULL_BACKUP_IN_PROGRESS_EVENT(EventSelectorUtil.selector(DatalakeFullBackupInProgressEvent.class)),
    DATALAKE_BACKUP_SUCCESS_EVENT(EventSelectorUtil.selector(DatalakeBackupSuccessEvent.class)),
    DATALAKE_DATABASE_BACKUP_FAILED_EVENT(EventSelectorUtil.selector(DatalakeDatabaseBackupFailedEvent.class)),
    DATALAKE_BACKUP_FAILED_EVENT(EventSelectorUtil.selector(DatalakeBackupFailedEvent.class)),
    DATALAKE_DATABASE_BACKUP_FINALIZED_EVENT("DATALAKE_DATABASE_BACKUP_FINALIZED_EVENT"),
    DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT("DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT"),
    DATALAKE_BACKUP_FAILURE_HANDLED_EVENT("DATALAKE_BACKUP_FAILURE_HANDLED_EVENT");

    private final String event;

    DatalakeBackupEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}