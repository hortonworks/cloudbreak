package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class EphemeralClusterUpdateRequest extends StackEvent {

    public EphemeralClusterUpdateRequest(Long stackId) {
        super(stackId);
    }
}
