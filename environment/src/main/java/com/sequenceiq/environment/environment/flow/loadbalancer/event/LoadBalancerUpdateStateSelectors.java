package com.sequenceiq.environment.environment.flow.loadbalancer.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum LoadBalancerUpdateStateSelectors implements FlowEvent {
    LOAD_BALANCER_UPDATE_START_EVENT,
    LOAD_BALANCER_STACK_UPDATE_EVENT,
    FINISH_LOAD_BALANCER_UPDATE_EVENT,
    FINALIZE_LOAD_BALANCER_UPDATE_EVENT,
    HANDLED_FAILED_LOAD_BALANCER_UPDATE_EVENT,
    FAILED_LOAD_BALANCER_UPDATE_EVENT;

    @Override
    public String event() {
        return name();
    }
}
