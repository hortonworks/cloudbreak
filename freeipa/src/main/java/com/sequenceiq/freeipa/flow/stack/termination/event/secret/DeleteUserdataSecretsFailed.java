package com.sequenceiq.freeipa.flow.stack.termination.event.secret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class DeleteUserdataSecretsFailed extends StackFailureEvent {

    @JsonCreator
    public DeleteUserdataSecretsFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
