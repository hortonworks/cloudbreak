package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class RepairSingleMasterInstanceEvent extends StackEvent {
    public RepairSingleMasterInstanceEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    public RepairSingleMasterInstanceEvent(String selector, Long stackId, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
    }

}
