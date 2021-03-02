package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SetupMonitoringRequest extends StackEvent {
    public SetupMonitoringRequest(Long stackId) {
        super(stackId);
    }
}