package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class PrepareProxyConfigFailed extends StackFailureEvent {
    public PrepareProxyConfigFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}