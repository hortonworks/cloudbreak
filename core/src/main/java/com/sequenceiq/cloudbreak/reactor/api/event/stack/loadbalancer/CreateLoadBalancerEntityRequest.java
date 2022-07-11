package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CreateLoadBalancerEntityRequest extends StackEvent {

    @JsonCreator
    public CreateLoadBalancerEntityRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
