package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class HandleClusterCreationSuccessSuccess extends StackEvent {
    public HandleClusterCreationSuccessSuccess(Long stackId) {
        super(stackId);
    }
}
