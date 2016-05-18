package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ConsulMetadataSetupSuccess extends StackEvent {
    public ConsulMetadataSetupSuccess(Long stackId) {
        super(stackId);
    }
}
