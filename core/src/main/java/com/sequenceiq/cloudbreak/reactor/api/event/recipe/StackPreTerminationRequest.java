package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackPreTerminationRequest extends StackEvent {

    private final boolean forced;

    @JsonCreator
    public StackPreTerminationRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") boolean forced) {
        super(stackId);
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }
}
