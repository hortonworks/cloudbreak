package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UserDataUpdateSuccess extends StackEvent {
    @JsonCreator
    public UserDataUpdateSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
