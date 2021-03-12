package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ConfigureClusterManagerManagementServicesSuccess extends StackEvent {
    public ConfigureClusterManagerManagementServicesSuccess(Long stackId) {
        super(stackId);
    }
}