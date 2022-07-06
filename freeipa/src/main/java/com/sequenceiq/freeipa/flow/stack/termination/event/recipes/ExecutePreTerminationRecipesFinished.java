package com.sequenceiq.freeipa.flow.stack.termination.event.recipes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class ExecutePreTerminationRecipesFinished extends TerminationEvent {

    @JsonCreator
    public ExecutePreTerminationRecipesFinished(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(ExecutePreTerminationRecipesFinished.class), stackId, forced);
    }

    @Override
    public String toString() {
        return "ExecutePreTerminationRecipesFinished{} " + super.toString();
    }

}
