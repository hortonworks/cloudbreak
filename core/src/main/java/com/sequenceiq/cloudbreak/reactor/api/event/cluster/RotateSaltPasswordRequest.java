package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RotateSaltPasswordRequest extends StackEvent {
    public RotateSaltPasswordRequest(Long stackId) {
        super(stackId);
    }
}
