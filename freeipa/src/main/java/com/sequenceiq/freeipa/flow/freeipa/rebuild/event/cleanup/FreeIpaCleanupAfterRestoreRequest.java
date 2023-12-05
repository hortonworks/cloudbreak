package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaCleanupAfterRestoreRequest extends StackEvent {

    @JsonCreator
    public FreeIpaCleanupAfterRestoreRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "FreeIpaCleanupAfterRestoreRequest{} " + super.toString();
    }
}
