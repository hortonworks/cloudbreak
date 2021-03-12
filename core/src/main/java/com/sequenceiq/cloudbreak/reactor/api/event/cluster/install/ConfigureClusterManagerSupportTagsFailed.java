package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ConfigureClusterManagerSupportTagsFailed extends StackFailureEvent {
    public ConfigureClusterManagerSupportTagsFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}