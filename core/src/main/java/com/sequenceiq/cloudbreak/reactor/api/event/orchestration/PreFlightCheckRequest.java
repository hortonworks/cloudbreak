package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PreFlightCheckRequest extends StackEvent {
    public PreFlightCheckRequest(Long stackId) {
        super(stackId);
    }
}
