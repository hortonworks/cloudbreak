package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterUpgradeResult extends ClusterPlatformResult<ClusterUpgradeRequest> {
    public ClusterUpgradeResult(ClusterUpgradeRequest request) {
        super(request);
    }

    public ClusterUpgradeResult(String statusReason, Exception errorDetails, ClusterUpgradeRequest request) {
        super(statusReason, errorDetails, request);
    }
}
