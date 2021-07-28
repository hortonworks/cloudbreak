package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DatalakeRecoveryRestoreComponentsSuccess extends StackEvent {

    public DatalakeRecoveryRestoreComponentsSuccess(Long stackId) {
        super(stackId);
    }
}
