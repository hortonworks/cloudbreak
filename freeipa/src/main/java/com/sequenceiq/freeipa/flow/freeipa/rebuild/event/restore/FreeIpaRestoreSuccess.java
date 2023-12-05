package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaRestoreSuccess extends StackEvent {
    @JsonCreator
    public FreeIpaRestoreSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
