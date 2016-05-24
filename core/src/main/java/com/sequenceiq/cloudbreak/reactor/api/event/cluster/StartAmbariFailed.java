package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class StartAmbariFailed extends StackFailureEvent {
    public StartAmbariFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
