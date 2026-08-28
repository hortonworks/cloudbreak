package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailureEvent;

public class LoadBalancerDeletionFailureEvent extends FreeIpaFailureEvent {

    @JsonCreator
    public LoadBalancerDeletionFailureEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("exception") Exception exception) {
        super(resourceId, failureType, exception);
    }

    @Override
    public String toString() {
        return "LoadBalancerDeletionFailureEvent{} " + super.toString();
    }
}
