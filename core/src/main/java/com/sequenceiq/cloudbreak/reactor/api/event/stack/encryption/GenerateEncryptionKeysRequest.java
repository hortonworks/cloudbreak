package com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class GenerateEncryptionKeysRequest extends StackEvent {
    @JsonCreator
    public GenerateEncryptionKeysRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
