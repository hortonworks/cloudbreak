package com.sequenceiq.freeipa.flow.freeipa.migration;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeFailedEvent;

public enum MultiAzMigrationFinalizeFlowEvent implements FlowEvent {

    MULTI_AZ_MIGRATION_FINALIZE_EVENT,
    MULTI_AZ_MIGRATION_FINALIZE_FINISHED_EVENT,
    MULTI_AZ_MIGRATION_FINALIZE_FAILURE_EVENT(EventSelectorUtil.selector(MultiAzMigrationFinalizeFailedEvent.class)),
    MULTI_AZ_MIGRATION_FINALIZE_FAIL_HANDLED_EVENT;

    private final String event;

    MultiAzMigrationFinalizeFlowEvent() {
        this.event = name();
    }

    MultiAzMigrationFinalizeFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
