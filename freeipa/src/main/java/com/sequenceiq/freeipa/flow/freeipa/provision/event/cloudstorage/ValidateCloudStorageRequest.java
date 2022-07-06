package com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ValidateCloudStorageRequest extends StackEvent {
    @JsonCreator
    public ValidateCloudStorageRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
