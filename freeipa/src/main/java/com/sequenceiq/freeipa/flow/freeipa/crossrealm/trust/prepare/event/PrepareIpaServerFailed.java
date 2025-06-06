package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class PrepareIpaServerFailed extends StackFailureEvent {

    @JsonCreator
    public PrepareIpaServerFailed(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
