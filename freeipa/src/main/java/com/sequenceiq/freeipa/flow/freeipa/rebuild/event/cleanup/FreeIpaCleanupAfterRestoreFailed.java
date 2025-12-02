package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FreeIpaCleanupAfterRestoreFailed extends StackFailureEvent {
    @JsonCreator
    public FreeIpaCleanupAfterRestoreFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failureType") FailureType failureType) {
        super(stackId, exception, failureType);
    }
}
