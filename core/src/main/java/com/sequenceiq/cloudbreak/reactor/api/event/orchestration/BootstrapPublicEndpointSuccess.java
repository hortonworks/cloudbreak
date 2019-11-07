package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapPublicEndpointSuccess extends StackEvent {
    public BootstrapPublicEndpointSuccess(Long stackId) {
        super(stackId);
    }
}
