package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapNewNodesEvent extends StackEvent {

    public BootstrapNewNodesEvent(Long stackId) {
        super(stackId);
    }

    public BootstrapNewNodesEvent(String selector, Long stackId) {
        super(selector, stackId);
    }
}
