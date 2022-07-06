package com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class HostMetadataSetupRequest extends StackEvent {
    @JsonCreator
    public HostMetadataSetupRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
