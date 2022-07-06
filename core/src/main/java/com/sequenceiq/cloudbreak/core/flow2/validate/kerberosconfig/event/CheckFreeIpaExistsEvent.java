package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CheckFreeIpaExistsEvent extends StackEvent {
    @JsonCreator
    public CheckFreeIpaExistsEvent(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
