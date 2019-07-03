package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyDeregisterSuccess extends StackEvent {

    public ClusterProxyDeregisterSuccess(Long stackId) {
        super(stackId);
    }
}
