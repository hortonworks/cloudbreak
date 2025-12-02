package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class LoadBalancerUpdateFailureEvent extends StackFailureEvent {

    @JsonCreator
    public LoadBalancerUpdateFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex, failureType);
    }
}
