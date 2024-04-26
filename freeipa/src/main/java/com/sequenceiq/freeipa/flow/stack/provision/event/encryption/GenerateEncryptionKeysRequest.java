package com.sequenceiq.freeipa.flow.stack.provision.event.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class GenerateEncryptionKeysRequest extends StackEvent {
    @JsonCreator
    public GenerateEncryptionKeysRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
