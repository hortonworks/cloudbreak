package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class WaitForClusterManagerSuccess extends StackEvent {
    public WaitForClusterManagerSuccess(Long stackId) {
        super(stackId);
    }
}
