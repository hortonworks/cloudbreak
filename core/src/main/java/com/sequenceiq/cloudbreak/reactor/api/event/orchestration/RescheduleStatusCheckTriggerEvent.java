package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class RescheduleStatusCheckTriggerEvent extends StackEvent {
    public RescheduleStatusCheckTriggerEvent(String selector, Long stackId, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }
}
