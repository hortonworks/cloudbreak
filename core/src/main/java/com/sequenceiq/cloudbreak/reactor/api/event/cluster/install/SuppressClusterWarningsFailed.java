package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class SuppressClusterWarningsFailed extends StackFailureEvent {
    public SuppressClusterWarningsFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
