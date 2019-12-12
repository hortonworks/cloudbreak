package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCertificateReissueSuccess extends StackEvent {
    public ClusterCertificateReissueSuccess(Long stackId) {
        super(stackId);
    }
}
