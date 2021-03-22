package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpscaleFailedConclusionRequest extends StackEvent {

    public ClusterUpscaleFailedConclusionRequest(Long stackId) {
        super(stackId);
    }

}