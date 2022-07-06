package com.sequenceiq.freeipa.flow.stack.termination.event.recipes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class ExecutePreTerminationRecipesRequest extends TerminationEvent {

    @JsonCreator
    public ExecutePreTerminationRecipesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(ExecutePreTerminationRecipesRequest.class), stackId, forced);
    }

}
