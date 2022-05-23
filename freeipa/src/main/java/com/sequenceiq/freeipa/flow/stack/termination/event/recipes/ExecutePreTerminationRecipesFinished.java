package com.sequenceiq.freeipa.flow.stack.termination.event.recipes;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class ExecutePreTerminationRecipesFinished extends TerminationEvent {

    public ExecutePreTerminationRecipesFinished(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(ExecutePreTerminationRecipesFinished.class), stackId, forced);
    }

    @Override
    public String toString() {
        return "ExecutePreTerminationRecipesFinished{} " + super.toString();
    }

}
