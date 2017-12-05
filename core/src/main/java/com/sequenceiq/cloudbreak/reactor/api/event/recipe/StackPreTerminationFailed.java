package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class StackPreTerminationFailed extends StackFailureEvent {

    public StackPreTerminationFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
