package com.sequenceiq.freeipa.flow.freeipa.rebuild.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailureEvent;

public class RebuildFailureEvent extends FreeIpaFailureEvent {

    @JsonCreator
    public RebuildFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("exception") Exception exception) {
        super(stackId, failureType, exception);
    }

    @Override
    public String toString() {
        return "RebuildFailureEvent{" + super.toString() + "} ";
    }
}
