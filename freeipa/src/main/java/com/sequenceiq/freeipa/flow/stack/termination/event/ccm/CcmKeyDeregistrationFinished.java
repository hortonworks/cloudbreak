package com.sequenceiq.freeipa.flow.stack.termination.event.ccm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class CcmKeyDeregistrationFinished extends TerminationEvent {
    @JsonCreator
    public CcmKeyDeregistrationFinished(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(CcmKeyDeregistrationFinished.class), stackId, forced);
    }
}
