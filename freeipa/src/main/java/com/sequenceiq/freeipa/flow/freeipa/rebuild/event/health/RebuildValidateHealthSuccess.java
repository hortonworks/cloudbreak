package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RebuildValidateHealthSuccess extends StackEvent {
    @JsonCreator
    public RebuildValidateHealthSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
