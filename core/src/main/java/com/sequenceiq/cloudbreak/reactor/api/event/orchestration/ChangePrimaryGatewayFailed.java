package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ChangePrimaryGatewayFailed extends StackFailureEvent {
    public ChangePrimaryGatewayFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
