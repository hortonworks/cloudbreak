package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class SetupMonitoringFailed extends StackFailureEvent {
    public SetupMonitoringFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}