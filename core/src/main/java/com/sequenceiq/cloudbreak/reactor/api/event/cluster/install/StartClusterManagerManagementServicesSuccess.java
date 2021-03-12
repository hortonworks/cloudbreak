package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterManagerManagementServicesSuccess extends StackEvent {
    public StartClusterManagerManagementServicesSuccess(Long stackId) {
        super(stackId);
    }
}
