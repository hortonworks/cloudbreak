package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;

public class StackTerminationFailureEvent extends StackFailureEvent {

    private final TerminationType terminationType;

    public StackTerminationFailureEvent(Long stackId, Exception exception, TerminationType terminationType) {
        super(stackId, exception);
        this.terminationType = terminationType;
    }

    public TerminationType getTerminationType() {
        return terminationType;
    }
}
