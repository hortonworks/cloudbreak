package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PreFlightCheckSuccess extends StackEvent {
    public PreFlightCheckSuccess(Long stackId) {
        super(stackId);
    }
}
