package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class HostMetadataSetupRequest extends StackEvent {
    public HostMetadataSetupRequest(Long stackId) {
        super(stackId);
    }
}
