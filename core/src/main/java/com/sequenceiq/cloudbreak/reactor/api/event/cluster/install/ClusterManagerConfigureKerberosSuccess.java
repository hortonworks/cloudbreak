package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerConfigureKerberosSuccess extends StackEvent {
    public ClusterManagerConfigureKerberosSuccess(Long stackId) {
        super(stackId);
    }
}
