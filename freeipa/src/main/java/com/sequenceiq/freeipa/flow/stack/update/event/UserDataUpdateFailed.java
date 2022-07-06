package com.sequenceiq.freeipa.flow.stack.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class UserDataUpdateFailed extends StackFailureEvent {
    public UserDataUpdateFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    @JsonCreator
    public UserDataUpdateFailed(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
    }
}
