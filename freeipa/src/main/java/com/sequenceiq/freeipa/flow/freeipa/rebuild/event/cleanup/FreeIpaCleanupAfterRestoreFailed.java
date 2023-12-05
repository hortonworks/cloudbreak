package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FreeIpaCleanupAfterRestoreFailed extends StackFailureEvent {
    @JsonCreator
    public FreeIpaCleanupAfterRestoreFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
