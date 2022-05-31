package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RotateSaltPasswordFailureResponse extends StackFailureEvent {
    public RotateSaltPasswordFailureResponse(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
