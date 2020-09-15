package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.FullRestoreInProgressEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatalakeRestoreFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatalakeRestoreSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatabaseRestoreEvent implements FlowEvent {
    DATABASE_RESTORE_IN_PROGRESS_EVENT("DATABASE_RESTORE_IN_PROGRESS_EVENT"),
    DATABASE_RESTORE_FAILED_EVENT(EventSelectorUtil.selector(DatalakeRestoreFailedEvent.class)),
    FULL_RESTORE_IN_PROGRESS_EVENT(EventSelectorUtil.selector(FullRestoreInProgressEvent.class)),
    RESTORE_FINISHED_EVENT(EventSelectorUtil.selector(DatalakeRestoreSuccess.class)),
    RESTORE_FINALIZED_EVENT("RESTORE_FINALIZED_EVENT"),
    RESTORE_FAIL_HANDLED_EVENT("RESTORE_FAIL_HANDLED_EVENT");

    private final String event;

    DatabaseRestoreEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}