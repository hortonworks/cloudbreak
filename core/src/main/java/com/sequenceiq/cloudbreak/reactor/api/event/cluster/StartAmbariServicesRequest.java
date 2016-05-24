package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariServicesRequest extends StackEvent {
    public StartAmbariServicesRequest(Long stackId) {
        super(stackId);
    }
}
