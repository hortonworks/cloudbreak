package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class DisableKerberosResult extends ClusterPlatformResult<DisableKerberosRequest> {

    public DisableKerberosResult(DisableKerberosRequest request) {
        super(request);
    }

    public DisableKerberosResult(String statusReason, Exception errorDetails, DisableKerberosRequest request) {
        super(statusReason, errorDetails, request);
    }

    @Override
    public String toString() {
        return "DisableKerberosResult{" + super.toString() + "}";
    }
}
