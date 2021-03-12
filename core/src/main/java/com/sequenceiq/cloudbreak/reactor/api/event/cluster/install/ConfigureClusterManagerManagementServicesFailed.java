package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ConfigureClusterManagerManagementServicesFailed extends StackFailureEvent {
    public ConfigureClusterManagerManagementServicesFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}