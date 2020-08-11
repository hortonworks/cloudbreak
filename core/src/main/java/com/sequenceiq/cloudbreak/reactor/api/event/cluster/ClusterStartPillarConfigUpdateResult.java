package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterStartPillarConfigUpdateResult extends ClusterPlatformResult<ClusterStartPillarConfigUpdateRequest> {

    public ClusterStartPillarConfigUpdateResult(ClusterStartPillarConfigUpdateRequest request) {
        super(request);
    }
}
