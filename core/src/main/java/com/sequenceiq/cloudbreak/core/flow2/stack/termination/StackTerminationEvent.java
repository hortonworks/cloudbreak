package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackTerminationEvent implements FlowEvent {
    TERMINATION_EVENT(FlowPhases.TERMINATION.name()),
    FORCE_TERMINATION_EVENT(FlowPhases.FORCED_TERMINATION.name()),
    TERMINATION_FINISHED_EVENT(TerminateStackResult.selector(TerminateStackResult.class)),
    TERMINATION_FAILED_EVENT(TerminateStackResult.failureSelector(TerminateStackResult.class)),
    STACK_TERMINATION_FINISHED_EVENT("TERMINATESTACKFINISHED"),
    STACK_TERMINATION_FAIL_HANDLED_EVENT("TERMINATIONFAILHANDLED");

    private String stringRepresentation;

    StackTerminationEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }

    public static StackTerminationEvent fromString(String stringRepresentiation) {
        for (StackTerminationEvent event : StackTerminationEvent.values()) {
            if (stringRepresentiation.equalsIgnoreCase(event.stringRepresentation)) {
                return event;
            }
        }
        return null;
    }
}
