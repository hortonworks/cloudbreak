package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class StackTerminationFailureEvent extends StackFailureEvent {

    public StackTerminationFailureEvent(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    @Override
    public String toString() {
        return "StackTerminationFailureEvent{} " + super.toString();
    }
}
