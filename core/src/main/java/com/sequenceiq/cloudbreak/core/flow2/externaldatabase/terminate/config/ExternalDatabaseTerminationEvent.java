package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.ExternalDatabaseTerminationStartEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalDatabaseTerminationEvent implements FlowEvent {

    START_EXTERNAL_DATABASE_TERMINATION_EVENT(ExternalDatabaseSelectableEvent.selector(ExternalDatabaseTerminationStartEvent.class)),
    EXTERNAL_DATABASE_WAIT_TERMINATION_SUCCESS_EVENT("TerminateExternalDatabaseResult"),
    EXTERNAL_DATABASE_TERMINATION_FAILED_EVENT("TerminateExternalDatabaseFailed"),
    EXTERNAL_DATABASE_TERMINATION_FINISHED_EVENT,
    EXTERNAL_DATABASE_TERMINATION_FAILURE_HANDLED_EVENT;

    private final String event;

    ExternalDatabaseTerminationEvent() {
        this.event = name();
    }

    ExternalDatabaseTerminationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
