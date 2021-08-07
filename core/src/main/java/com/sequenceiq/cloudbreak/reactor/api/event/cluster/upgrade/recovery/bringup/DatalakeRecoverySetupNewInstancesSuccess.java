package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DatalakeRecoverySetupNewInstancesSuccess extends StackEvent {

    public DatalakeRecoverySetupNewInstancesSuccess(Long stackId) {
        super(stackId);
    }
}
