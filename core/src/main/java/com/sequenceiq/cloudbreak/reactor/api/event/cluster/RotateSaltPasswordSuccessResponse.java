package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RotateSaltPasswordSuccessResponse extends StackEvent {
    public RotateSaltPasswordSuccessResponse(Long stackId) {
        super(stackId);
    }
}
