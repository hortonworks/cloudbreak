package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerConfigureKerberosRequest extends StackEvent {
    public ClusterManagerConfigureKerberosRequest(Long stackId) {
        super(stackId);
    }
}
