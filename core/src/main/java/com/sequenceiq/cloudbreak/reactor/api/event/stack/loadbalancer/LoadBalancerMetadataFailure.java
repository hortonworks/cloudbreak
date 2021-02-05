package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class LoadBalancerMetadataFailure extends StackFailureEvent {
    public LoadBalancerMetadataFailure(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
