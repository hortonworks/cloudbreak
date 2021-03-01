package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class CreateCloudLoadBalancersFailure extends StackFailureEvent {
    public CreateCloudLoadBalancersFailure(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}

