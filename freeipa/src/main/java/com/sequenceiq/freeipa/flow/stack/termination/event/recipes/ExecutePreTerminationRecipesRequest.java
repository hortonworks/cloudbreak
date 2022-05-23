package com.sequenceiq.freeipa.flow.stack.termination.event.recipes;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class ExecutePreTerminationRecipesRequest extends TerminationEvent {

    public ExecutePreTerminationRecipesRequest(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(ExecutePreTerminationRecipesRequest.class), stackId, forced);
    }

}
