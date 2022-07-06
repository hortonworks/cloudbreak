package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class MountDisksSuccess extends StackEvent {
    @JsonCreator
    public MountDisksSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
