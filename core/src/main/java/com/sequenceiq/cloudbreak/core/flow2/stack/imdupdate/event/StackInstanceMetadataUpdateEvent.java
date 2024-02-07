package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum StackInstanceMetadataUpdateEvent implements FlowEvent {
    STACK_IMDUPDATE_EVENT,
    STACK_IMDUPDATE_FINISHED_EVENT(EventSelectorUtil.selector(StackInstanceMetadataUpdateResult.class)),

    STACK_IMDUPDATE_FINALIZED_EVENT,
    STACK_IMDUPDATE_FAILURE_EVENT(EventSelectorUtil.selector(StackInstanceMetadataUpdateFailureEvent.class)),
    STACK_IMDUPDATE_FAIL_HANDLED_EVENT;

    private final String event;

    StackInstanceMetadataUpdateEvent() {
        this.event = name();
    }

    StackInstanceMetadataUpdateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
