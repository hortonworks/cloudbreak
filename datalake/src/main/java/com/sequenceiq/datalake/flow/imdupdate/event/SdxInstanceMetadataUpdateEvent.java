package com.sequenceiq.datalake.flow.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxInstanceMetadataUpdateEvent extends SdxEvent {

    private final InstanceMetadataUpdateType updateType;

    @JsonCreator
    public SdxInstanceMetadataUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("updateType") InstanceMetadataUpdateType updateType) {
        super(selector, sdxId, userId);
        this.updateType = updateType;
    }

    public InstanceMetadataUpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxInstanceMetadataUpdateEvent.class, other,
                event -> updateType == event.getUpdateType());
    }

    @Override
    public String toString() {
        return "SdxInstanceMetadataUpdateEvent{" +
                "updateType=" + updateType +
                "} " + super.toString();
    }

}
