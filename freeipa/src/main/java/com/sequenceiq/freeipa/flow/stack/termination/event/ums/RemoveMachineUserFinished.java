package com.sequenceiq.freeipa.flow.stack.termination.event.ums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class RemoveMachineUserFinished extends TerminationEvent {

    @JsonCreator
    public RemoveMachineUserFinished(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(RemoveMachineUserFinished.class), stackId, forced);
    }
}
