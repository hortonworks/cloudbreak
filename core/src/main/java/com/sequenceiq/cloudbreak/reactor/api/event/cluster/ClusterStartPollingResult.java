package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterStartPollingResult extends ClusterPlatformResult<ClusterStartPollingRequest> {

    public ClusterStartPollingResult(ClusterStartPollingRequest request) {
        super(request);
    }

    public ClusterStartPollingResult(String statusReason, Exception errorDetails, ClusterStartPollingRequest request) {
        super(statusReason, errorDetails, request);
    }
}
