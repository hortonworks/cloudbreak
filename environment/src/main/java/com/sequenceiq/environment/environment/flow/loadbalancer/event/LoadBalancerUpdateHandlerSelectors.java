package com.sequenceiq.environment.environment.flow.loadbalancer.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum  LoadBalancerUpdateHandlerSelectors implements FlowEvent {
    ENVIRONMENT_UPDATE_HANDLER_EVENT,
    STACK_UPDATE_HANDLER_EVENT;

    @Override
    public String event() {
        return name();
    }
}