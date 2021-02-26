package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RegisterFreeIpaDnsFailure extends StackFailureEvent {
    public RegisterFreeIpaDnsFailure(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
