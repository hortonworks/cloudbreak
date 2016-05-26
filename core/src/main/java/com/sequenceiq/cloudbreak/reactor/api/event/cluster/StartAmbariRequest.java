package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariRequest extends StackEvent {
    public StartAmbariRequest(Long stackId) {
        super(stackId);
    }
}
