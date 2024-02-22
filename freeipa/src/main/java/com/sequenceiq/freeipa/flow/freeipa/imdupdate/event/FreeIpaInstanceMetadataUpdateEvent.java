package com.sequenceiq.freeipa.flow.freeipa.imdupdate.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum FreeIpaInstanceMetadataUpdateEvent implements FlowEvent {
    STACK_IMDUPDATE_EVENT,
    STACK_IMDUPDATE_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaInstanceMetadataUpdateResult.class)),

    STACK_IMDUPDATE_FINALIZED_EVENT,
    STACK_IMDUPDATE_FAILURE_EVENT(EventSelectorUtil.selector(FreeIpaInstanceMetadataUpdateFailureEvent.class)),
    STACK_IMDUPDATE_FAIL_HANDLED_EVENT;

    private final String event;

    FreeIpaInstanceMetadataUpdateEvent() {
        this.event = name();
    }

    FreeIpaInstanceMetadataUpdateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
