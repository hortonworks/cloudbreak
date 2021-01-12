package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapFreeIPAEndpointSuccess extends StackEvent {
    public BootstrapFreeIPAEndpointSuccess(Long stackId) {
        super(stackId);
    }
}
