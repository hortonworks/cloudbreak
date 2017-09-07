package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackTerminationEvent implements FlowEvent {
    TERMINATION_EVENT("STACK_TERMINATE_TRIGGER_EVENT"),
    FORCE_TERMINATION_EVENT("STACK_FORCE_TERMINATE_TRIGGER_EVENT"),
    TERMINATION_FINISHED_EVENT(TerminateStackResult.selector(TerminateStackResult.class)),
    TERMINATION_FAILED_EVENT(TerminateStackResult.failureSelector(TerminateStackResult.class)),
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
