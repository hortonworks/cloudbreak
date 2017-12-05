package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackPreTerminationRequest extends StackEvent {

    public StackPreTerminationRequest(Long stackId) {
        super(stackId);
    }

}
