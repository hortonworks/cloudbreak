package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class WaitForAmbariServerSuccess extends StackEvent {
    public WaitForAmbariServerSuccess(Long stackId) {
        super(stackId);
    }
}
