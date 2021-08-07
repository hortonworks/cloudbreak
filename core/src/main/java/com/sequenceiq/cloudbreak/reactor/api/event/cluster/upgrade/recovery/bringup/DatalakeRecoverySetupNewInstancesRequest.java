package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DatalakeRecoverySetupNewInstancesRequest extends StackEvent {

    public DatalakeRecoverySetupNewInstancesRequest(Long stackId) {
        super(stackId);
    }
}
