package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterDownscaleFailedConclusionRequest extends StackEvent  {

    public ClusterDownscaleFailedConclusionRequest(Long stackId) {
        super(stackId);
    }

}
