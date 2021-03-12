package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerSetupMonitoringRequest extends StackEvent {
    public ClusterManagerSetupMonitoringRequest(Long stackId) {
        super(stackId);
    }
}