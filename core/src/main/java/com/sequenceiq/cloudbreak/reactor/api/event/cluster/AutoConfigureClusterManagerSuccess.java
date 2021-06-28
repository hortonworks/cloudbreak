package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AutoConfigureClusterManagerSuccess extends StackEvent {
    public AutoConfigureClusterManagerSuccess(Long stackId) {
        super(stackId);
    }
}
