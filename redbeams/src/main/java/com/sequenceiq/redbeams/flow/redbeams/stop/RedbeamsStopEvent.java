package com.sequenceiq.redbeams.flow.redbeams.stop;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerSuccess;

public enum RedbeamsStopEvent implements FlowEvent {

    REDBEAMS_STOP_EVENT(),
    STOP_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(StopDatabaseServerSuccess.class)),
    STOP_DATABASE_SERVER_FAILED_EVENT(EventSelectorUtil.selector(StopDatabaseServerFailed.class)),
    REDBEAMS_STOP_FAILED_EVENT(),
    REDBEAMS_STOP_FINISHED_EVENT(),
    REDBEAMS_STOP_FAILURE_HANDLED_EVENT();

    private final String event;

    RedbeamsStopEvent() {
        event = name();
    }

    RedbeamsStopEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
