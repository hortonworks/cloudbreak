package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class UpscaleCheckHostMetadataResult extends ClusterPlatformResult<UpscaleCheckHostMetadataRequest> {
    public UpscaleCheckHostMetadataResult(UpscaleCheckHostMetadataRequest request) {
        super(request);
    }

    public UpscaleCheckHostMetadataResult(String statusReason, Exception errorDetails, UpscaleCheckHostMetadataRequest request) {
        super(statusReason, errorDetails, request);
    }
}
