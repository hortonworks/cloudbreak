package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class PrepareDatalakeResourceFailed extends StackFailureEvent {
    public PrepareDatalakeResourceFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
