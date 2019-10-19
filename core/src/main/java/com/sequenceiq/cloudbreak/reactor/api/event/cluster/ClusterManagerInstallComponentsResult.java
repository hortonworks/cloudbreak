package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerInstallComponentsResult extends AbstractClusterScaleResult<ClusterManagerInstallComponentsRequest> {
    public ClusterManagerInstallComponentsResult(ClusterManagerInstallComponentsRequest request) {
        super(request);
    }

    public ClusterManagerInstallComponentsResult(String statusReason, Exception errorDetails, ClusterManagerInstallComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
