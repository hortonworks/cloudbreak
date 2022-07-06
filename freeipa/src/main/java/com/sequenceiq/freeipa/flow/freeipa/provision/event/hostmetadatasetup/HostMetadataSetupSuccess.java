package com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class HostMetadataSetupSuccess extends StackEvent {
    @JsonCreator
    public HostMetadataSetupSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
