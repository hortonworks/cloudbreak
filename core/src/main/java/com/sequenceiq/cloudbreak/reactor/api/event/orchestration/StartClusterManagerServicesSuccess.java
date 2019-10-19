package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterManagerServicesSuccess extends StackEvent {
    public StartClusterManagerServicesSuccess(Long stackId) {
        super(stackId);
    }

    public StartClusterManagerServicesSuccess(String selector, Long stackId) {
        super(selector, stackId);
    }

}
