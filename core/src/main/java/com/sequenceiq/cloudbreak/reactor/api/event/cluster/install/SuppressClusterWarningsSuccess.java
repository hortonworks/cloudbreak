package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SuppressClusterWarningsSuccess extends StackEvent {
    public SuppressClusterWarningsSuccess(Long stackId) {
        super(stackId);
    }
}
