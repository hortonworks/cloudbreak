package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum FreeIPAVerticalScaleEvent implements FlowEvent {
    STACK_VERTICALSCALE_EVENT("STACK_VERTICAL_SCALE_TRIGGER_EVENT"),
    STACK_VERTICALSCALE_FINISHED_EVENT(EventSelectorUtil.selector(FreeIPAVerticalScaleResult.class)),
    STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(FreeIPAVerticalScaleRequest.class)),

    STACK_VERTICALSCALE_FINALIZED_EVENT,
    STACK_VERTICALSCALE_FAILURE_EVENT,
    STACK_VERTICALSCALE_FAIL_HANDLED_EVENT("STACKVERTICALSCALEFAILHANDLEDEVENT");

    private final String event;

    FreeIPAVerticalScaleEvent() {
        this.event = name();
    }

    FreeIPAVerticalScaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
