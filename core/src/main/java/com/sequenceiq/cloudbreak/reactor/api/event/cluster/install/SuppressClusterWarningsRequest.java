package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SuppressClusterWarningsRequest extends StackEvent {
    public SuppressClusterWarningsRequest(Long stackId) {
        super(stackId);
    }
}
