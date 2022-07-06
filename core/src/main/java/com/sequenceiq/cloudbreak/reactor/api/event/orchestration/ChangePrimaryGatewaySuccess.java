package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ChangePrimaryGatewaySuccess extends StackEvent {

    private final String newPrimaryGatewayFQDN;

    @JsonCreator
    public ChangePrimaryGatewaySuccess(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("newPrimaryGatewayFQDN") String newPrimaryGatewayFQDN) {
        super(stackId);
        this.newPrimaryGatewayFQDN = newPrimaryGatewayFQDN;
    }

    public String getNewPrimaryGatewayFQDN() {
        return newPrimaryGatewayFQDN;
    }
}
