package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterManagerManagementServicesRequest extends StackEvent {
    public StartClusterManagerManagementServicesRequest(Long stackId) {
        super(stackId);
    }
}
