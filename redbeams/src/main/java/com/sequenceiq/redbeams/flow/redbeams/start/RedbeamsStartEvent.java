package com.sequenceiq.redbeams.flow.redbeams.start;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerSuccess;

public enum RedbeamsStartEvent implements FlowEvent {

    REDBEAMS_START_EVENT(),
    START_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(StartDatabaseServerSuccess.class)),
    START_DATABASE_SERVER_FAILED_EVENT(EventSelectorUtil.selector(StartDatabaseServerFailed.class)),
    REDBEAMS_START_FAILED_EVENT(),
    REDBEAMS_START_FINISHED_EVENT(),
    REDBEAMS_START_FAILURE_HANDLED_EVENT();

    private final String event;

    RedbeamsStartEvent() {
        event = name();
    }

    RedbeamsStartEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
