package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdateClusterConfigSuccess extends StackEvent {
    public UpdateClusterConfigSuccess(Long stackId) {
        super(stackId);
    }
}
