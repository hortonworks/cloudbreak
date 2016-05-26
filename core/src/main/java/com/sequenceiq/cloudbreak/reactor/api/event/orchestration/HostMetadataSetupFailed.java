package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class HostMetadataSetupFailed extends StackFailureEvent {
    public HostMetadataSetupFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
