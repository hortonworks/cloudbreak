package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariInitComponentsResult extends AbstractClusterScaleResult<AmbariInitComponentsRequest> {

    public AmbariInitComponentsResult(AmbariInitComponentsRequest request) {
        super(request);
    }

    public AmbariInitComponentsResult(String statusReason, Exception errorDetails, AmbariInitComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
