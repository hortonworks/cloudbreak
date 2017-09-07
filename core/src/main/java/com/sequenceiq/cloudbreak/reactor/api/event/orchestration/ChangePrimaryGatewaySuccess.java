package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ChangePrimaryGatewaySuccess extends StackEvent {

    private final String newPrimaryGatewayFQDN;

    public ChangePrimaryGatewaySuccess(Long stackId, String newPrimaryGatewayFQDN) {
        super(stackId);
        this.newPrimaryGatewayFQDN = newPrimaryGatewayFQDN;
    }

    public String getNewPrimaryGatewayFQDN() {
        return newPrimaryGatewayFQDN;
    }
}
