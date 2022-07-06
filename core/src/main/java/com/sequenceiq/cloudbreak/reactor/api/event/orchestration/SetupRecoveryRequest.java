package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;

public class SetupRecoveryRequest extends StackEvent {

    private final ProvisionType provisionType;

    @JsonCreator
    public SetupRecoveryRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("provisionType") ProvisionType provisionType) {
        super(stackId);
        this.provisionType = provisionType;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
