package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class LoadBalancerProvisionSuccess extends StackEvent {
    @JsonCreator
    public LoadBalancerProvisionSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
