package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyDeregisterRequest extends StackEvent {

    public ClusterProxyDeregisterRequest(Long stackId) {
        super(stackId);
    }

}
