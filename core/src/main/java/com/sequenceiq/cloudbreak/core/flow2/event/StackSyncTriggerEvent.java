package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class StackSyncTriggerEvent extends StackEvent {

    private final Boolean statusUpdateEnabled;

    public StackSyncTriggerEvent(String selector, Long stackId, Boolean statusUpdateEnabled) {
        super(selector, stackId);
        this.statusUpdateEnabled = statusUpdateEnabled;
    }

    public StackSyncTriggerEvent(String selector, Long stackId, Boolean statusUpdateEnabled, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.statusUpdateEnabled = statusUpdateEnabled;
    }

    public Boolean getStatusUpdateEnabled() {
        return statusUpdateEnabled;
    }
}
