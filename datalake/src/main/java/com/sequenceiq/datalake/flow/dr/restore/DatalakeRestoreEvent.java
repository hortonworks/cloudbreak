package com.sequenceiq.datalake.flow.dr.restore;

import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreCouldNotStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeFullRestoreInProgressEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeRestoreEvent implements FlowEvent {
    DATALAKE_DATABASE_RESTORE_EVENT(DatalakeDatabaseRestoreStartEvent.class),
    DATALAKE_TRIGGER_RESTORE_EVENT(),
    DATALAKE_RESTORE_SERVICES_STOPPED_EVENT(),
    DATALAKE_DATABASE_RESTORE_COULD_NOT_START_EVENT(DatalakeDatabaseRestoreCouldNotStartEvent.class),
    DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT(),
    DATALAKE_FULL_RESTORE_IN_PROGRESS_EVENT(DatalakeFullRestoreInProgressEvent.class),
    DATALAKE_RESTORE_SUCCESS_EVENT(DatalakeRestoreSuccessEvent.class),
    DATALAKE_DATABASE_RESTORE_FAILED_EVENT(DatalakeDatabaseRestoreFailedEvent.class),
    DATALAKE_RESTORE_FAILED_EVENT(DatalakeRestoreFailedEvent.class),
    DATALAKE_RESTORE_FINALIZED_EVENT(),
    DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT(),
    DATALAKE_RESTORE_FAILURE_HANDLED_EVENT();

    private final String event;

    DatalakeRestoreEvent() {
        event = name();
    }

    DatalakeRestoreEvent(Class eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}
