package com.sequenceiq.freeipa.flow.stack.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UserDataUpdateSuccess extends StackEvent {
    @JsonCreator
    public UserDataUpdateSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
