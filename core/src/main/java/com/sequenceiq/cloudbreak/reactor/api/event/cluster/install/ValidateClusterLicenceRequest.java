package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ValidateClusterLicenceRequest extends StackEvent {
    public ValidateClusterLicenceRequest(Long stackId) {
        super(stackId);
    }
}
