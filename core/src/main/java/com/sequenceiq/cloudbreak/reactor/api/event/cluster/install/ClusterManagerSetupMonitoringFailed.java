package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterManagerSetupMonitoringFailed extends StackFailureEvent {
    public ClusterManagerSetupMonitoringFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}