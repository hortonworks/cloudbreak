package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FinishCrossRealmTrustAddTrustFailed extends StackFailureEvent {

    @JsonCreator
    public FinishCrossRealmTrustAddTrustFailed(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
