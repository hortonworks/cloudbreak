package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerSetupMonitoringSuccess extends StackEvent {
    public ClusterManagerSetupMonitoringSuccess(Long stackId) {
        super(stackId);
    }
}
