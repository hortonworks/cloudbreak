package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.ExternalDatabaseCommenceStartEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalDatabaseStartEvent implements FlowEvent {

    EXTERNAL_DATABASE_COMMENCE_START_EVENT(ExternalDatabaseSelectableEvent.selector(ExternalDatabaseCommenceStartEvent.class)),
    EXTERNAL_DATABASE_STARTED_EVENT("StartExternalDatabaseResult"),
    EXTERNAL_DATABASE_START_FAILED_EVENT("StartExternalDatabaseFailed"),
    EXTERNAL_DATABASE_START_FINALIZED_EVENT,
    EXTERNAL_DATABASE_START_FAILURE_HANDLED_EVENT;

    private final String event;

    ExternalDatabaseStartEvent() {
        this.event = name();
    }

    ExternalDatabaseStartEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
