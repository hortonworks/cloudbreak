package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpdateServiceConfigFailure extends StackFailureEvent {
    public UpdateServiceConfigFailure(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
