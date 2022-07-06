package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;

public class FinalizeClusterInstallRequest extends StackEvent {

    private final ProvisionType provisionType;

    @JsonCreator
    public FinalizeClusterInstallRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("provisionType") ProvisionType provisionType) {
        super(stackId);
        this.provisionType = provisionType;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
