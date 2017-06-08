package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class EphemeralClusterUpdateTriggerEvent extends StackEvent {
    public EphemeralClusterUpdateTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
    }
}
