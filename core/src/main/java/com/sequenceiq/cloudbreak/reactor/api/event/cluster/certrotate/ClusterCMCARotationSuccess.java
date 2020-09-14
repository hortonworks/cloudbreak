package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCMCARotationSuccess extends StackEvent {
    public ClusterCMCARotationSuccess(Long stackId) {
        super(stackId);
    }
}
