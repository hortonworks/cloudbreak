package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterCertificatesRotationFailed extends StackFailureEvent {
    public ClusterCertificatesRotationFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    public ClusterCertificatesRotationFailed(String selector, Long stackId, Exception exception) {
        super(selector, stackId, exception);
    }
}
