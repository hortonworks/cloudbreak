package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class PillarConfigUpdateFailed extends StackFailureEvent {

    public PillarConfigUpdateFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}