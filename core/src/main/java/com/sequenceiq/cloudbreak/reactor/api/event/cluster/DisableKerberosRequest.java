package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class DisableKerberosRequest extends ClusterPlatformRequest {

    @JsonCreator
    public DisableKerberosRequest(@JsonProperty("stackId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "DisableKerberosRequest{" + super.toString() + "}";
    }
}
