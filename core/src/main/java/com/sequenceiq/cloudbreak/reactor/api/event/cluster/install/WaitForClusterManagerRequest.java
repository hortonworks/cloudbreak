package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class WaitForClusterManagerRequest extends StackEvent {
    public WaitForClusterManagerRequest(Long stackId) {
        super(stackId);
    }
}