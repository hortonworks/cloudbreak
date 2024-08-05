package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class LoadBalancerConfigurationSuccess extends StackEvent {
    @JsonCreator
    public LoadBalancerConfigurationSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
