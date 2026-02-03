package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class LoadBalancerDomainUpdateSuccess extends StackEvent {
    @JsonCreator
    public LoadBalancerDomainUpdateSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
