package com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ValidateCloudStorageFailed extends StackFailureEvent {
    @JsonCreator
    public ValidateCloudStorageFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
