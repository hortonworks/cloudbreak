package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.ExternalDatabaseCreationStartEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalDatabaseCreationEvent implements FlowEvent {

    START_EXTERNAL_DATABASE_CREATION_EVENT(ExternalDatabaseSelectableEvent.selector(ExternalDatabaseCreationStartEvent.class)),
    EXTERNAL_DATABASE_WAIT_SUCCESS_EVENT("CreateExternalDatabaseResult"),
    EXTERNAL_DATABASE_CREATION_FAILED_EVENT("CreateExternalDatabaseFailed"),
    EXTERNAL_DATABASE_CREATION_FINISHED_EVENT,
    EXTERNAL_DATABASE_CREATION_FAILURE_HANDLED_EVENT;

    private final String event;

    ExternalDatabaseCreationEvent() {
        this.event = name();
    }

    ExternalDatabaseCreationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
