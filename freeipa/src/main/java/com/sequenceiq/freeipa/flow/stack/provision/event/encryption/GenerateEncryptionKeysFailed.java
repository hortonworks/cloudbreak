package com.sequenceiq.freeipa.flow.stack.provision.event.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class GenerateEncryptionKeysFailed extends StackFailureEvent {
    @JsonCreator
    public GenerateEncryptionKeysFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
