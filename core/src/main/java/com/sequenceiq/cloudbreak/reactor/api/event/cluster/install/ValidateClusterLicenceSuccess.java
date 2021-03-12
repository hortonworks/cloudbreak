package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ValidateClusterLicenceSuccess extends StackEvent {
    public ValidateClusterLicenceSuccess(Long stackId) {
        super(stackId);
    }
}
