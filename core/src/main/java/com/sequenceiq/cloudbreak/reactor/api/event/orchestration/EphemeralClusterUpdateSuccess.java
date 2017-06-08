package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class EphemeralClusterUpdateSuccess extends StackEvent {
    public EphemeralClusterUpdateSuccess(Long stackId) {
        super(stackId);
    }
}
