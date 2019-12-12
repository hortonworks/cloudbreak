package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCertificateReissueRequest extends StackEvent {
    public ClusterCertificateReissueRequest(Long stackId) {
        super(stackId);
    }
}
