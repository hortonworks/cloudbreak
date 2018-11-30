package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariInstallComponentsResult extends AbstractClusterScaleResult<AmbariInstallComponentsRequest> {
    public AmbariInstallComponentsResult(AmbariInstallComponentsRequest request) {
        super(request);
    }

    public AmbariInstallComponentsResult(String statusReason, Exception errorDetails, AmbariInstallComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
