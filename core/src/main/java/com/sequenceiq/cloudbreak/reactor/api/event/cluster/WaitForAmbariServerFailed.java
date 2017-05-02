package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class WaitForAmbariServerFailed extends StackFailureEvent {
    public WaitForAmbariServerFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
