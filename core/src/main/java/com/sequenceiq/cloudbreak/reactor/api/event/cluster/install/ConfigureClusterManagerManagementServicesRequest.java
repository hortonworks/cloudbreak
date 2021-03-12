package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ConfigureClusterManagerManagementServicesRequest extends StackEvent {
    public ConfigureClusterManagerManagementServicesRequest(Long stackId) {
        super(stackId);
    }
}
