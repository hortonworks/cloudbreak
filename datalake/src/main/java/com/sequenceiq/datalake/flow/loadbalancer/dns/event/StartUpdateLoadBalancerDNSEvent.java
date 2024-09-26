package com.sequenceiq.datalake.flow.loadbalancer.dns.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StartUpdateLoadBalancerDNSEvent extends SdxEvent {
    @JsonCreator
    public StartUpdateLoadBalancerDNSEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxName") String sdxName,
            @JsonProperty("userId") String userId) {
        super(selector, sdxId, sdxName, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(StartUpdateLoadBalancerDNSEvent.class, other, event -> Objects.equals(event.getResourceId(), other.getResourceId()));
    }

    @Override
    public String toString() {
        return selector() + "{sdxId: '" + getResourceId() + "'}";
    }
}
