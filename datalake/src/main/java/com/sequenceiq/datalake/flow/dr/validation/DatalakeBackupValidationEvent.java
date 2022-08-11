package com.sequenceiq.datalake.flow.dr.validation;

import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeBackupValidationFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeBackupValidationEvent implements FlowEvent {

    DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT(),
    DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_EVENT(),
    DATALAKE_BACKUP_VALIDATION_SUCCESS_EVENT(),
    DATALAKE_BACKUP_VALIDATION_FAILED_EVENT(DatalakeBackupValidationFailedEvent.class),
    DATALAKE_BACKUP_VALIDATION_FAILURE_HANDLED_EVENT(),
    DATALAKE_BACKUP_VALIDATION_FINALIZED_EVENT();

    private final String event;

    DatalakeBackupValidationEvent() {
        this.event = name();
    }

    DatalakeBackupValidationEvent(Class eventClass) {
        this.event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}