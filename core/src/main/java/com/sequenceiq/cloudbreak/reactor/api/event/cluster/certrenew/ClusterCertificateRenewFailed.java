package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterCertificateRenewFailed extends StackFailureEvent {
    public ClusterCertificateRenewFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    public ClusterCertificateRenewFailed(String selector, Long stackId, Exception exception) {
        super(selector, stackId, exception);
    }
}
