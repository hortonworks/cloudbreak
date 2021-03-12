package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class PrepareExtendedTemplateFailed extends StackFailureEvent {
    public PrepareExtendedTemplateFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}