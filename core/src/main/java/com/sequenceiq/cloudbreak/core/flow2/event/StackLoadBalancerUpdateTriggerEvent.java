package com.sequenceiq.cloudbreak.core.flow2.event;

import reactor.rx.Promise;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackLoadBalancerUpdateTriggerEvent extends StackEvent {

    public StackLoadBalancerUpdateTriggerEvent(Long stackId) {
        super(stackId);
    }

    public StackLoadBalancerUpdateTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    public StackLoadBalancerUpdateTriggerEvent(String selector, Long stackId, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }
}
