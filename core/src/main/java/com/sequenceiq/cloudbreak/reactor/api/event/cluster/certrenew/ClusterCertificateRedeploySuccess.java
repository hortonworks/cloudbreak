package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCertificateRedeploySuccess extends StackEvent {
    public ClusterCertificateRedeploySuccess(Long stackId) {
        super(stackId);
    }
}
