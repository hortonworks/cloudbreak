package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RebuildValidateHealthRequest extends StackEvent {

    @JsonCreator
    public RebuildValidateHealthRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "FreeIpaCleanupAfterRestoreRequest{} " + super.toString();
    }
}
