package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class DeregisterServicesResult extends ClusterPlatformResult<DeregisterServicesRequest> {

    public DeregisterServicesResult(DeregisterServicesRequest request) {
        super(request);
    }

    public DeregisterServicesResult(String statusReason, Exception errorDetails, DeregisterServicesRequest request) {
        super(statusReason, errorDetails, request);
    }

    @Override
    public String toString() {
        return "DisableKerberosResult{" + super.toString() + "}";
    }
}
