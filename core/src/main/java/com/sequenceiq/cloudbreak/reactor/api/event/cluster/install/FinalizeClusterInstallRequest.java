package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;

public class FinalizeClusterInstallRequest extends StackEvent {

    private final ProvisionType provisionType;

    public FinalizeClusterInstallRequest(Long stackId, ProvisionType provisionType) {
        super(stackId);
        this.provisionType = provisionType;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
