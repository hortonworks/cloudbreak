package com.sequenceiq.datalake.flow.datalake.kraftmigration;

import com.sequenceiq.datalake.flow.datalake.kraftmigration.event.DatalakeKraftMigrationFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeKraftMigrationEvent implements FlowEvent {

    DATALAKE_KRAFT_MIGRATION_TRIGGER_EVENT,
    DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_EVENT,
    DATALAKE_KRAFT_MIGRATION_SUCCESS_EVENT,
    DATALAKE_KRAFT_MIGRATION_FAILED_EVENT(EventSelectorUtil.selector(DatalakeKraftMigrationFailedEvent.class)),
    DATALAKE_KRAFT_MIGRATION_FAILED_HANDLED_EVENT,
    DATALAKE_KRAFT_MIGRATION_FINALIZED_EVENT;

    private final String event;

    DatalakeKraftMigrationEvent() {
        event = name();
    }

    DatalakeKraftMigrationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
