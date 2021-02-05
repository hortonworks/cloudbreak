package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartCmForLbSuccess extends StackEvent {
    public RestartCmForLbSuccess(Long stackId) {
        super(stackId);
    }
}