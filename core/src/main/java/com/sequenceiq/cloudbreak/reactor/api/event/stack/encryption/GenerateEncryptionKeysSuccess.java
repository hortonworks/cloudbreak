package com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class GenerateEncryptionKeysSuccess extends StackEvent {
    @JsonCreator
    public GenerateEncryptionKeysSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
