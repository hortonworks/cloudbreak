package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ConsulMetadataSetupRequest extends StackEvent {
    public ConsulMetadataSetupRequest(Long stackId) {
        super(stackId);
    }
}
