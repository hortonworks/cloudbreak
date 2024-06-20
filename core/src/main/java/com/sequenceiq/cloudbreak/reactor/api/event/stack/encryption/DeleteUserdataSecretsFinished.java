package com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class DeleteUserdataSecretsFinished extends StackEvent {

    @JsonCreator
    public DeleteUserdataSecretsFinished(
            @JsonProperty("resourceId") Long stackId) {
        super(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class), stackId);
    }
}
