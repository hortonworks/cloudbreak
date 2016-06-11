package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariServicesSuccess extends StackEvent {
    public StartAmbariServicesSuccess(Long stackId) {
        super(stackId);
    }
}
