package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PrepareProxyConfigSuccess extends StackEvent {
    public PrepareProxyConfigSuccess(Long stackId) {
        super(stackId);
    }
}
