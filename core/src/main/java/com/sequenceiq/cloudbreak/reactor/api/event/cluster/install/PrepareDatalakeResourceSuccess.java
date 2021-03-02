package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PrepareDatalakeResourceSuccess extends StackEvent {
    public PrepareDatalakeResourceSuccess(Long stackId) {
        super(stackId);
    }
}
