package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterRequest extends StackEvent {
    public StartClusterRequest(Long stackId) {
        super(stackId);
    }
}
