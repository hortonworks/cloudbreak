package com.sequenceiq.datalake.flow.refresh;

import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshStartEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatahubRefreshFlowEvent implements FlowEvent {
    DATAHUB_REFRESH_START_EVENT(DatahubRefreshStartEvent.class),
    DATAHUB_REFRESH_IN_PROGRESS_EVENT,
    DATAHUB_REFRESH_FINISHED_EVENT,
    DATAHUB_REFRESH_FINALIZED_EVENT,
    DATAHUB_REFRESH_FAILED_EVENT(DatahubRefreshFailedEvent.class),
    DATAHUB_REFRESH_FAILED_HANDLED_EVENT;


    private final String event;

    DatahubRefreshFlowEvent(Class<?> clazz) {
        event = EventSelectorUtil.selector(clazz);

    }

    DatahubRefreshFlowEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
