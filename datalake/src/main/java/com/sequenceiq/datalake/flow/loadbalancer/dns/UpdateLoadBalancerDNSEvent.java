package com.sequenceiq.datalake.flow.loadbalancer.dns;

import com.sequenceiq.datalake.flow.loadbalancer.dns.event.UpdateLoadBalancerDNSFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpdateLoadBalancerDNSEvent implements FlowEvent {
    UPDATE_LOAD_BALANCER_DNS_PEM_EVENT(),
    UPDATE_LOAD_BALANCER_DNS_PEM_SUCCESS_EVENT(),
    UPDATE_LOAD_BALANCER_DNS_IPA_EVENT(),
    UPDATE_LOAD_BALANCER_DNS_IPA_SUCCESS_EVENT(),
    UPDATE_LOAD_BALANCER_DNS_FAILED_EVENT(UpdateLoadBalancerDNSFailedEvent.class),
    UPDATE_LOAD_BALANCER_DNS_FAILURE_HANDLED_EVENT();

    private final String event;

    UpdateLoadBalancerDNSEvent() {
        event = name();
    }

    UpdateLoadBalancerDNSEvent(Class eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}
