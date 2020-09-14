package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterHostCertificatesRotationSuccess extends StackEvent {
    public ClusterHostCertificatesRotationSuccess(Long stackId) {
        super(stackId);
    }
}
