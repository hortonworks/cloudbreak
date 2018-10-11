package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class MaintenanceModeValidationTriggerEvent extends StackEvent {

    public MaintenanceModeValidationTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    public MaintenanceModeValidationTriggerEvent(String selector, Long stackId, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
    }

}
