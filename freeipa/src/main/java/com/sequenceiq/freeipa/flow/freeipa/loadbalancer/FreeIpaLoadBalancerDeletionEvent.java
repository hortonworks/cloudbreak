package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerCloudDeletionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerDeregistrationSuccess;

public enum FreeIpaLoadBalancerDeletionEvent implements FlowEvent {
    LOAD_BALANCER_DELETION_EVENT,
    LOAD_BALANCER_DNS_DEREGISTRATION_FINISHED_EVENT(EventSelectorUtil.selector(LoadBalancerDeregistrationSuccess.class)),
    LOAD_BALANCER_CLOUD_DELETION_FINISHED_EVENT(EventSelectorUtil.selector(LoadBalancerCloudDeletionSuccess.class)),
    LOAD_BALANCER_DELETION_FINISHED_EVENT,
    LOAD_BALANCER_DELETION_FAILURE_EVENT(EventSelectorUtil.selector(LoadBalancerDeletionFailureEvent.class)),
    LOAD_BALANCER_DELETION_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaLoadBalancerDeletionEvent() {
        this.event = name();
    }

    FreeIpaLoadBalancerDeletionEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
