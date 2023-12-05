package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaCleanupAfterRestoreSuccess extends StackEvent {
    @JsonCreator
    public FreeIpaCleanupAfterRestoreSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
