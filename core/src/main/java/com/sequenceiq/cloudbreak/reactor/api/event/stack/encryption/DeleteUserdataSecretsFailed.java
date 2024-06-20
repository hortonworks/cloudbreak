package com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class DeleteUserdataSecretsFailed extends StackFailureEvent {

    @JsonCreator
    public DeleteUserdataSecretsFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
