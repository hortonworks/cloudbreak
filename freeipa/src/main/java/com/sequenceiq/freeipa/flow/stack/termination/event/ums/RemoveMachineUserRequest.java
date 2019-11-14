package com.sequenceiq.freeipa.flow.stack.termination.event.ums;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class RemoveMachineUserRequest extends TerminationEvent {

    public RemoveMachineUserRequest(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(RemoveMachineUserRequest.class), stackId, forced);
    }
}
