package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterSuccess extends StackEvent {
    public StartClusterSuccess(Long stackId) {
        super(stackId);
    }

    public StartClusterSuccess(String selector, Long stackId) {
        super(selector, stackId);
    }
}
