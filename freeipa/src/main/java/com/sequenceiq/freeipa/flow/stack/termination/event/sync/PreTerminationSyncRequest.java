package com.sequenceiq.freeipa.flow.stack.termination.event.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class PreTerminationSyncRequest extends TerminationEvent {

    @JsonCreator
    public PreTerminationSyncRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(PreTerminationSyncRequest.class), stackId, forced);
    }
}
