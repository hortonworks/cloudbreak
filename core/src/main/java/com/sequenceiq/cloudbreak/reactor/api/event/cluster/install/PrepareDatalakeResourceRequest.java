package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PrepareDatalakeResourceRequest extends StackEvent {
    public PrepareDatalakeResourceRequest(Long stackId) {
        super(stackId);
    }
}
