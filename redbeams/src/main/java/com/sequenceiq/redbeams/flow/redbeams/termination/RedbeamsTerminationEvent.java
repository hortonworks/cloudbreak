package com.sequenceiq.redbeams.flow.redbeams.termination;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerSuccess;

public enum RedbeamsTerminationEvent implements FlowEvent {
    REDBEAMS_TERMINATION_EVENT("REDBEAMS_TERMINATION_EVENT"),
    TERMINATE_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(TerminateDatabaseServerSuccess.class)),
    TERMINATE_DATABASE_SERVER_FAILED_EVENT(EventSelectorUtil.selector(TerminateDatabaseServerFailed.class)),
    DEREGISTER_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(DeregisterDatabaseServerSuccess.class)),
    DEREGISTER_DATABASE_SERVER_FAILED_EVENT(EventSelectorUtil.selector(DeregisterDatabaseServerFailed.class)),
    REDBEAMS_TERMINATION_FAILED_EVENT("REDBEAMS_TERMINATION_FAILED_EVENT"),
    REDBEAMS_TERMINATION_FINISHED_EVENT("REDBEAMS_TERMINATION_FINISHED_EVENT"),
    REDBEAMS_TERMINATION_FAILURE_HANDLED_EVENT("REDBEAMS_TERMINATION_FAILURE_HANDLED_EVENT");

    private final String event;

    RedbeamsTerminationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
