package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import reactor.rx.Promise;

public class WaitForAmbariServerRequest extends StackEvent {
    public WaitForAmbariServerRequest(Long stackId) {
        super(stackId);
    }

    public WaitForAmbariServerRequest(String selector, Long stackId) {
        super(selector, stackId);
    }

    public WaitForAmbariServerRequest(String selector, Long stackId, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
    }
}
