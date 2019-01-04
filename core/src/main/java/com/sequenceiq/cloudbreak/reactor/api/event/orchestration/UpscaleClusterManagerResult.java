package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UpscaleClusterManagerResult extends AbstractClusterScaleResult<UpscaleClusterManagerRequest> {

    public UpscaleClusterManagerResult(UpscaleClusterManagerRequest request) {
        super(request);
    }

    public UpscaleClusterManagerResult(String statusReason, Exception errorDetails, UpscaleClusterManagerRequest request) {
        super(statusReason, errorDetails, request);
    }
}
