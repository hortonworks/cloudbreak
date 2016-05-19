package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class HostMetadataSetupSuccess extends StackEvent {
    public HostMetadataSetupSuccess(Long stackId) {
        super(stackId);
    }
}
