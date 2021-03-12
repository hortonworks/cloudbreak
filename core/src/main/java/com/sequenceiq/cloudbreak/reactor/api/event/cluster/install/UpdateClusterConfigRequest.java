package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdateClusterConfigRequest extends StackEvent {
    public UpdateClusterConfigRequest(Long stackId) {
        super(stackId);
    }
}
