package com.sequenceiq.freeipa.flow.freeipa.migration;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitResult;

public enum MultiAzMigrationInitFlowEvent implements FlowEvent {

    MULTI_AZ_MIGRATION_INIT_EVENT,
    MULTI_AZ_MIGRATION_INIT_RESULT_EVENT(EventSelectorUtil.selector(MultiAzMigrationInitResult.class)),
    MULTI_AZ_MIGRATION_INIT_FINISHED_EVENT,
    MULTI_AZ_MIGRATION_INIT_FAILURE_EVENT(EventSelectorUtil.selector(MultiAzMigrationInitFailedEvent.class)),
    MULTI_AZ_MIGRATION_INIT_FAIL_HANDLED_EVENT;

    private final String event;

    MultiAzMigrationInitFlowEvent() {
        this.event = name();
    }

    MultiAzMigrationInitFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
