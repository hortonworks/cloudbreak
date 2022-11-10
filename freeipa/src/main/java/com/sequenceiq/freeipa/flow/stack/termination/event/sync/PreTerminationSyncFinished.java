package com.sequenceiq.freeipa.flow.stack.termination.event.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class PreTerminationSyncFinished extends TerminationEvent {

    @JsonCreator
    public PreTerminationSyncFinished(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(PreTerminationSyncFinished.class), stackId, forced);
    }

    @Override
    public String toString() {
        return "PreTerminationSyncFinished{} " + super.toString();
    }
}
