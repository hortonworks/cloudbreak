package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RestartCmForLbFailure extends StackFailureEvent {
    public RestartCmForLbFailure(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
