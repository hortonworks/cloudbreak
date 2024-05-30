package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaRestoreRequest extends StackEvent {

    @JsonCreator
    public FreeIpaRestoreRequest(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "FreeIpaRestoreRequest{} " + super.toString();
    }
}
