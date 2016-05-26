package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterSyncResult extends ClusterPlatformResult<ClusterSyncRequest> {
    public ClusterSyncResult(ClusterSyncRequest request) {
        super(request);
    }

    public ClusterSyncResult(String statusReason, Exception errorDetails, ClusterSyncRequest request) {
        super(statusReason, errorDetails, request);
    }
}
