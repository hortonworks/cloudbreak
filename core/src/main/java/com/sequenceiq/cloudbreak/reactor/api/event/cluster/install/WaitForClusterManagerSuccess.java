package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class WaitForClusterManagerSuccess extends StackEvent {
    public WaitForClusterManagerSuccess(Long stackId) {
        super(stackId);
    }
}
