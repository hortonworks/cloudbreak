package com.sequenceiq.datalake.flow.loadbalancer.dns.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class StartUpdateLoadBalancerDNSEvent extends SdxEvent {
    public StartUpdateLoadBalancerDNSEvent(String selector, Long sdxId, String userId, Promise<AcceptResult> accepted) {
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
