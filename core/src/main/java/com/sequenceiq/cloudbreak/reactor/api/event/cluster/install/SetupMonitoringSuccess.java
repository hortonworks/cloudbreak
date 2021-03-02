package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SetupMonitoringSuccess extends StackEvent {
    public SetupMonitoringSuccess(Long stackId) {
        super(stackId);
    }
}
