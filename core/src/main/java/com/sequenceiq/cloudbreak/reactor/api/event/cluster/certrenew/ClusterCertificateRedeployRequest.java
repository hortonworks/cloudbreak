package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCertificateRedeployRequest extends StackEvent {
    public ClusterCertificateRedeployRequest(Long stackId) {
        super(stackId);
    }
}
