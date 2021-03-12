package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ConfigureClusterManagerSupportTagsSuccess extends StackEvent {
    public ConfigureClusterManagerSupportTagsSuccess(Long stackId) {
        super(stackId);
    }
}