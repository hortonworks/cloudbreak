package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariServicesSuccess extends StackEvent {
    public StartAmbariServicesSuccess(Long stackId) {
        super(stackId);
    }

    public StartAmbariServicesSuccess(String selector, Long stackId) {
        super(selector, stackId);
    }

}
