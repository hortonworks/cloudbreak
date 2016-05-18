package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ConsulMetadataSetupFailed extends StackFailureEvent {
    public ConsulMetadataSetupFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
