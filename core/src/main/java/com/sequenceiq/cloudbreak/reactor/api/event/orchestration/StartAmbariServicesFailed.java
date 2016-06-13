package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class StartAmbariServicesFailed extends StackFailureEvent {
    public StartAmbariServicesFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
