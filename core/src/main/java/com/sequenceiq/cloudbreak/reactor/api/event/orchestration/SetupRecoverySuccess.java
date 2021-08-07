package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SetupRecoverySuccess extends StackEvent {

    public SetupRecoverySuccess(Long stackId) {
        super(stackId);
    }
}
