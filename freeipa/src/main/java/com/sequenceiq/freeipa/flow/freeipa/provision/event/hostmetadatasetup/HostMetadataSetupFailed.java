package com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class HostMetadataSetupFailed extends StackFailureEvent {
    @JsonCreator
    public HostMetadataSetupFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
