package com.sequenceiq.datalake.flow.atlas.updated;

import com.sequenceiq.datalake.flow.atlas.updated.event.CheckAtlasUpdatedFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum CheckAtlasUpdatedEvent implements FlowEvent {
    CHECK_ATLAS_UPDATED_EVENT(),
    CHECK_ATLAS_UPDATED_SUCCESS_EVENT(),
    CHECK_ATLAS_UPDATED_FAILED_EVENT(CheckAtlasUpdatedFailedEvent.class),
    CHECK_ATLAS_UPDATED_FAILURE_HANDLED_EVENT();

    private final String event;

    CheckAtlasUpdatedEvent() {
        event = name();
    }

    CheckAtlasUpdatedEvent(Class eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}
