package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseSelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.ExternalDatabaseCommenceStopEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum ExternalDatabaseStopEvent implements FlowEvent {

    EXTERNAL_DATABASE_COMMENCE_STOP_EVENT(ExternalDatabaseSelectableEvent.selector(ExternalDatabaseCommenceStopEvent.class)),
    EXTERNAL_DATABASE_STOPPED_EVENT("StopExternalDatabaseResult"),
    EXTERNAL_DATABASE_STOP_FAILED_EVENT("StopExternalDatabaseFailed"),
    EXTERNAL_DATABASE_STOP_FINALIZED_EVENT,
    EXTERNAL_DATABASE_STOP_FAILURE_HANDLED_EVENT;

    private final String event;

    ExternalDatabaseStopEvent() {
        this.event = name();
    }

    ExternalDatabaseStopEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
