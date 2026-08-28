package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class LoadBalancerDeregistrationSuccess extends StackEvent {

    @JsonCreator
    public LoadBalancerDeregistrationSuccess(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "LoadBalancerDeregistrationSuccess{} " + super.toString();
    }
}
