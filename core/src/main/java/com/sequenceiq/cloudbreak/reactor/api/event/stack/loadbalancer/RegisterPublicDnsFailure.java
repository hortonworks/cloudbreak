package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RegisterPublicDnsFailure extends StackFailureEvent {
    public RegisterPublicDnsFailure(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
