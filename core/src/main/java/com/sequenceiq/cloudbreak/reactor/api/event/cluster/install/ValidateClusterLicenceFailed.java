package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ValidateClusterLicenceFailed extends StackFailureEvent {
    public ValidateClusterLicenceFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
