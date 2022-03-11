package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterServicesRestartPollingResult extends ClusterPlatformResult<ClusterStartPollingRequest> {

    public ClusterServicesRestartPollingResult(ClusterStartPollingRequest request) {
        super(request);
    }

    public ClusterServicesRestartPollingResult(String statusReason, Exception errorDetails, ClusterStartPollingRequest request) {
        super(statusReason, errorDetails, request);
    }
}
