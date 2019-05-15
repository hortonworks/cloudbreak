package com.sequenceiq.freeipa.flow.stack.termination;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum StackTerminationEvent implements FlowEvent {
    TERMINATION_EVENT("STACK_TERMINATE_TRIGGER_EVENT"),
    TERMINATION_FINISHED_EVENT(EventSelectorUtil.selector(TerminateStackResult.class)),
    TERMINATION_FAILED_EVENT(EventSelectorUtil.failureSelector(TerminateStackResult.class)),
    TERMINATION_FINALIZED_EVENT("TERMINATESTACKFINALIZED"),
    STACK_TERMINATION_FAIL_HANDLED_EVENT("TERMINATIONFAILHANDLED");

    private final String event;

    StackTerminationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
