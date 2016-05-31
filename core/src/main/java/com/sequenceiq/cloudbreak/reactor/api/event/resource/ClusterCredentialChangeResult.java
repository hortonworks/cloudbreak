package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterCredentialChangeResult extends ClusterPlatformResult<ClusterCredentialChangeRequest> {
    public ClusterCredentialChangeResult(ClusterCredentialChangeRequest request) {
        super(request);
    }

    public ClusterCredentialChangeResult(String statusReason, Exception errorDetails, ClusterCredentialChangeRequest request) {
        super(statusReason, errorDetails, request);
    }
}
