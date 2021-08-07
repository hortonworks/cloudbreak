package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;

public class SetupRecoveryRequest extends StackEvent {

    private final ProvisionType provisionType;

    public SetupRecoveryRequest(Long stackId, ProvisionType provisionType) {
        super(stackId);
        this.provisionType = provisionType;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
