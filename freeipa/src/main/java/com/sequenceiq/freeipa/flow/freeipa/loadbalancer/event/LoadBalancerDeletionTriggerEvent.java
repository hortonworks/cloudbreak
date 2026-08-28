package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class LoadBalancerDeletionTriggerEvent extends StackEvent {

    @JsonCreator
    public LoadBalancerDeletionTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId) {
        super(selector, resourceId);
    }

    @Override
    public String toString() {
        return "LoadBalancerDeletionTriggerEvent{} " + super.toString();
    }
}
