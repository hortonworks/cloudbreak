package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailureEvent;

public class LoadBalancerCreationFailureEvent extends FreeIpaFailureEvent {

    @JsonCreator
    public LoadBalancerCreationFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("exception") Exception ex) {
        super(stackId, failureType, ex);
    }

    @Override
    public String toString() {
        return "LoadBalancerCreationFailureEvent{" +
                super.toString() +
                '}';
    }
}
