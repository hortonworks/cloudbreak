package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ChangePrimaryGatewayRequest extends StackEvent {
    public ChangePrimaryGatewayRequest(Long stackId) {
        super(stackId);
    }
}
