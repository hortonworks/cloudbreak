package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariSuccess extends StackEvent {
    public StartAmbariSuccess(Long stackId) {
        super(stackId);
    }
}
