package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ResetJvmParamsRequest extends ClusterPlatformRequest {

    @JsonCreator
    public ResetJvmParamsRequest(@JsonProperty("stackId") Long stackId) {
        super(stackId);
    }
}
