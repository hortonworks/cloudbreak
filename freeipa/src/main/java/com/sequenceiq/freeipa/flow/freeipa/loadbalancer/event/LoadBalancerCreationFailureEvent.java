package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class LoadBalancerCreationFailureEvent extends StackFailureEvent {

    @JsonCreator
    public LoadBalancerCreationFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex, failureType);
    }

    @Override
    public String toString() {
        return "LoadBalancerCreationFailureEvent{" + super.toString() + '}';
    }
}
