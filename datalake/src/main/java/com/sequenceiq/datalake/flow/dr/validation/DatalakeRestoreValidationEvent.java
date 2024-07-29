package com.sequenceiq.datalake.flow.dr.validation;

import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeRestoreValidationFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeRestoreValidationEvent implements FlowEvent {

    DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT(),
    DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_EVENT(),
    DATALAKE_RESTORE_VALIDATION_SUCCESS_EVENT(),
    DATALAKE_RESTORE_VALIDATION_FAILED_EVENT(DatalakeRestoreValidationFailedEvent.class),
    DATALAKE_RESTORE_VALIDATION_FAILURE_HANDLED_EVENT(),
    DATALAKE_RESTORE_VALIDATION_FINALIZED_EVENT();

    private final String event;

    DatalakeRestoreValidationEvent() {
        event = name();
    }

    DatalakeRestoreValidationEvent(Class<?> eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}