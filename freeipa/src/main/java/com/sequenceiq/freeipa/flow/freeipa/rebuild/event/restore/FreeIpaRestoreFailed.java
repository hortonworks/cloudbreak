package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FreeIpaRestoreFailed extends StackFailureEvent {
    @JsonCreator
    public FreeIpaRestoreFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
