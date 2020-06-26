package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PillarConfigUpdateSuccess extends StackEvent {

    public PillarConfigUpdateSuccess(Long stackId) {
        super(stackId);
    }
}