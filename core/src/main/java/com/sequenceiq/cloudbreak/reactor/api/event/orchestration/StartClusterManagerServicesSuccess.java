package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterManagerServicesSuccess extends StackEvent {
    public StartClusterManagerServicesSuccess(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public StartClusterManagerServicesSuccess(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }

}
