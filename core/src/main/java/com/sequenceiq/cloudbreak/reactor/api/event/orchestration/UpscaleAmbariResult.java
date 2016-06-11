package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UpscaleAmbariResult extends AbstractClusterScaleResult<UpscaleAmbariRequest> {

    public UpscaleAmbariResult(UpscaleAmbariRequest request) {
        super(request);
    }

    public UpscaleAmbariResult(String statusReason, Exception errorDetails, UpscaleAmbariRequest request) {
        super(statusReason, errorDetails, request);
    }
}
