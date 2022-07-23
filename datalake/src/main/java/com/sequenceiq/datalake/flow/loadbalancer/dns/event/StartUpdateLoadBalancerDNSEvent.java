package com.sequenceiq.datalake.flow.loadbalancer.dns.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class StartUpdateLoadBalancerDNSEvent extends SdxEvent {
    @JsonCreator
    public StartUpdateLoadBalancerDNSEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
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
