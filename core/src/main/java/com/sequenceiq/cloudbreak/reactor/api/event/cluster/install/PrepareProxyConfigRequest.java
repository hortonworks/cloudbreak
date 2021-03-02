package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PrepareProxyConfigRequest extends StackEvent {
    public PrepareProxyConfigRequest(Long stackId) {
        super(stackId);
    }
}
