package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackPreTerminationSuccess extends StackEvent {

    public StackPreTerminationSuccess(Long stackId) {
        super(stackId);
    }
}
