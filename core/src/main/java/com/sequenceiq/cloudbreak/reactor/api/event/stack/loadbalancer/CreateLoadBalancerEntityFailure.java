package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class CreateLoadBalancerEntityFailure extends StackFailureEvent {
    public CreateLoadBalancerEntityFailure(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
