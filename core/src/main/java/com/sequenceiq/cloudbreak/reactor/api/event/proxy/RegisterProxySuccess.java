package com.sequenceiq.cloudbreak.reactor.api.event.proxy;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RegisterProxySuccess extends StackEvent {
    public RegisterProxySuccess(Long stackId) {
        super(stackId);
    }

    public RegisterProxySuccess(String selector, Long stackId) {
        super(selector, stackId);
    }

}
