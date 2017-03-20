package com.sequenceiq.cloudbreak.reactor.api.event.proxy;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RegisterProxyRequest extends StackEvent {
    public RegisterProxyRequest(Long stackId) {
        super(stackId);
    }
}
