package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareIpaServerRequest extends StackEvent {

    @JsonCreator
    public PrepareIpaServerRequest(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
