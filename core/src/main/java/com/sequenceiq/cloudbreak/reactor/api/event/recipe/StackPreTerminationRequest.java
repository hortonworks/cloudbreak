package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackPreTerminationRequest extends StackEvent {

    private final boolean forced;

    public StackPreTerminationRequest(Long stackId, boolean forced) {
        super(stackId);
        this.forced = forced;
    }

    public boolean getForced() {
        return forced;
    }
}
