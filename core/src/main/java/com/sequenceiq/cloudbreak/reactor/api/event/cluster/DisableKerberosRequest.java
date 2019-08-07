package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class DisableKerberosRequest extends ClusterPlatformRequest {

    public DisableKerberosRequest(Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "DisableKerberosRequest{" + super.toString() + "}";
    }
}
