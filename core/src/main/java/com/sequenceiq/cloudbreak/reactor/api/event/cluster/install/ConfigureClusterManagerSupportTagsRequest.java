package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ConfigureClusterManagerSupportTagsRequest extends StackEvent {
    public ConfigureClusterManagerSupportTagsRequest(Long stackId) {
        super(stackId);
    }
}
