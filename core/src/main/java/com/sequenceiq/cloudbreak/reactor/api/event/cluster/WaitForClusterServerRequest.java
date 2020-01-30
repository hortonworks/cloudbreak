package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class WaitForClusterServerRequest extends StackEvent {
    public WaitForClusterServerRequest(Long stackId) {
        super(stackId);
    }

    public WaitForClusterServerRequest(String selector, Long stackId) {
        super(selector, stackId);
    }

    public WaitForClusterServerRequest(String selector, Long stackId, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }
}
