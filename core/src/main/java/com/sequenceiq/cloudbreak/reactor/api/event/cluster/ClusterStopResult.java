package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterStopResult extends ClusterPlatformResult<ClusterStopRequest> {
    public ClusterStopResult(ClusterStopRequest request) {
        super(request);
    }

    public ClusterStopResult(String statusReason, Exception errorDetails, ClusterStopRequest request) {
        super(statusReason, errorDetails, request);
    }
}
