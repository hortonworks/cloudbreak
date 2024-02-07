package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackInstanceMetadataUpdateTriggerEvent extends StackEvent {

    private final InstanceMetadataUpdateType updateType;

    @JsonCreator
    public StackInstanceMetadataUpdateTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("updateType") InstanceMetadataUpdateType updateType) {
        super(selector, stackId);
        this.updateType = updateType;
    }

    public InstanceMetadataUpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public String toString() {
        return "FreeIpaInstanceMetadataUpdateTriggerEvent{" +
                ", updateType=" + updateType +
                "} " + super.toString();
    }
}
