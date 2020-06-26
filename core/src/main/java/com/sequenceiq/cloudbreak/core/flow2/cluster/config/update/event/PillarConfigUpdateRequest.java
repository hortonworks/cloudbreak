package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PillarConfigUpdateRequest extends StackEvent {

    public PillarConfigUpdateRequest(Long stackId) {
        super(stackId);
    }
}