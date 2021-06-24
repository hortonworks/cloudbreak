package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AutoConfigureClusterManagerRequest extends StackEvent {
    public AutoConfigureClusterManagerRequest(Long stackId) {
        super(stackId);
    }
}
