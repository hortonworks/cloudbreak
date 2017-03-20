package com.sequenceiq.cloudbreak.reactor.api.event.proxy;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RegisterProxyFailed extends StackFailureEvent {
    public RegisterProxyFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
