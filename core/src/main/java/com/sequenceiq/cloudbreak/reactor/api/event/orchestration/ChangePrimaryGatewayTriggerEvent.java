package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import reactor.rx.Promise;

public class ChangePrimaryGatewayTriggerEvent extends StackEvent {
    public ChangePrimaryGatewayTriggerEvent(String selector, Long stackId, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
    }
}
