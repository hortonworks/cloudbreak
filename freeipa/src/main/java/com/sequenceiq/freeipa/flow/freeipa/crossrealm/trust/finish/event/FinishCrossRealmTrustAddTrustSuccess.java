package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FinishCrossRealmTrustAddTrustSuccess extends StackEvent {

    @JsonCreator
    public FinishCrossRealmTrustAddTrustSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
