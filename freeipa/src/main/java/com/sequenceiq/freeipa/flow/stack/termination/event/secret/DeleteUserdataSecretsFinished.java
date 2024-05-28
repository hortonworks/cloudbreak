package com.sequenceiq.freeipa.flow.stack.termination.event.secret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class DeleteUserdataSecretsFinished extends TerminationEvent {

    @JsonCreator
    public DeleteUserdataSecretsFinished(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class), stackId, forced);
    }
}
