package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum FreeIpaVerticalScaleEvent implements FlowEvent {
    STACK_VERTICALSCALE_EVENT,
    STACK_VERTICALSCALE_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaVerticalScaleResult.class)),
    STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(FreeIpaVerticalScaleRequest.class)),

    STACK_VERTICALSCALE_FINALIZED_EVENT,
    STACK_VERTICALSCALE_FAILURE_EVENT(EventSelectorUtil.selector(FreeIpaVerticalScaleFailureEvent.class)),
    STACK_VERTICALSCALE_FAIL_HANDLED_EVENT;

    private final String event;

    FreeIpaVerticalScaleEvent() {
        this.event = name();
    }

    FreeIpaVerticalScaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
