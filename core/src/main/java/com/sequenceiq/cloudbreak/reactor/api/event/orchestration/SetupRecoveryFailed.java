package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class SetupRecoveryFailed extends StackFailureEvent {

    public SetupRecoveryFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
