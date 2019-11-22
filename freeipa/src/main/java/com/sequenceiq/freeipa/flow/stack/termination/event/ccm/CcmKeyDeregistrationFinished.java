package com.sequenceiq.freeipa.flow.stack.termination.event.ccm;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class CcmKeyDeregistrationFinished extends TerminationEvent {
    public CcmKeyDeregistrationFinished(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(CcmKeyDeregistrationFinished.class), stackId, forced);
    }
}
