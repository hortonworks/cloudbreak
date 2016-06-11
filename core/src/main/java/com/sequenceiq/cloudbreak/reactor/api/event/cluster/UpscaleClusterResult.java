package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UpscaleClusterResult extends AbstractClusterScaleResult<UpscaleClusterRequest> {

    public UpscaleClusterResult(UpscaleClusterRequest request) {
        super(request);
    }

    public UpscaleClusterResult(String statusReason, Exception errorDetails, UpscaleClusterRequest request) {
        super(statusReason, errorDetails, request);
    }
}
