package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartClusterManagerServerSuccess extends StackEvent {
    public RestartClusterManagerServerSuccess(Long stackId) {
        super(stackId);
    }
}
