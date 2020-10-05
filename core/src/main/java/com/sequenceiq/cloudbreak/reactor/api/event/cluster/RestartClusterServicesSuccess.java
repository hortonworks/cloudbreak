package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartClusterServicesSuccess extends StackEvent {
    public RestartClusterServicesSuccess(Long stackId) {
        super(stackId);
    }
}
