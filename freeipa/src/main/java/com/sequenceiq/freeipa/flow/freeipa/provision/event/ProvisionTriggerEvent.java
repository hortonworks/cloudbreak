package com.sequenceiq.freeipa.flow.freeipa.provision.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ProvisionTriggerEvent extends StackEvent {

    private FreeIpaLoadBalancerType loadBalancer;

    @JsonCreator
    public ProvisionTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("loadBalancer") FreeIpaLoadBalancerType loadBalancer) {
        super(selector, stackId);
        this.loadBalancer = loadBalancer;
    }

    public FreeIpaLoadBalancerType getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(FreeIpaLoadBalancerType loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ProvisionTriggerEvent.class, other, event -> loadBalancer == event.loadBalancer);
    }

    @Override
    public String toString() {
        return "ProvisionTriggerEvent{" +
                "loadBalancer=" + loadBalancer +
                "} " + super.toString();
    }
}
