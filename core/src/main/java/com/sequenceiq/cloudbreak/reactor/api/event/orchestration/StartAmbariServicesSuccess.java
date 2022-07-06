package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariServicesSuccess extends StackEvent {
    public StartAmbariServicesSuccess(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public StartAmbariServicesSuccess(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }

}
