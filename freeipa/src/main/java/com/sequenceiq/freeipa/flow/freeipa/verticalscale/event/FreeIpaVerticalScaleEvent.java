package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum FreeIpaVerticalScaleEvent implements FlowEvent {
    STACK_VERTICALSCALE_EVENT,
    STACK_VERTICALSCALE_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaVerticalScaleResult.class)),
    STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(FreeIpaVerticalScaleFailureEvent.class)),

    FINALIZED_EVENT("STACKVERTICALSCALEFINALIZEDEVENT"),
    FAILURE_EVENT("STACKVERTICALSCALEFAILUREEVENT"),
    STACK_VERTICALSCALE_FAIL_HANDLED_EVENT("STACKVERTICALSCALEFAILHANDLEDEVENT");

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
