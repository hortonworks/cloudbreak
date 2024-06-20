package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class StackPreTerminationSuccess extends TerminationEvent {

    @JsonCreator
    public StackPreTerminationSuccess(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("terminationType") TerminationType terminationType) {
        super(EventSelectorUtil.selector(StackPreTerminationSuccess.class), stackId, terminationType);
    }
}
