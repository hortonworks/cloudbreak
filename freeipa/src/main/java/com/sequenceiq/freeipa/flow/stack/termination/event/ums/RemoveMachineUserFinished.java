package com.sequenceiq.freeipa.flow.stack.termination.event.ums;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class RemoveMachineUserFinished extends TerminationEvent {

    public RemoveMachineUserFinished(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(RemoveMachineUserFinished.class), stackId, forced);
    }
}
