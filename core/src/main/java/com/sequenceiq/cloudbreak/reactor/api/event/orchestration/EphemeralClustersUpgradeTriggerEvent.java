package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class EphemeralClustersUpgradeTriggerEvent extends StackEvent {
    public EphemeralClustersUpgradeTriggerEvent(String selector, Long stackId, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
    }
}
