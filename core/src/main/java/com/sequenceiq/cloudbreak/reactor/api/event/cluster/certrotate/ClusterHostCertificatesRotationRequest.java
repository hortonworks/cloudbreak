package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterHostCertificatesRotationRequest extends StackEvent {
    public ClusterHostCertificatesRotationRequest(Long stackId) {
        super(stackId);
    }
}
