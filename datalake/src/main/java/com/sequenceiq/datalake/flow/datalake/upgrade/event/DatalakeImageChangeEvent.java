package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_IMAGE_CHANGE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeImageChangeEvent extends SdxEvent {

    private final String imageId;

    @JsonCreator
    public DatalakeImageChangeEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId) {
        super(sdxId, userId);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String selector() {
        return DATALAKE_IMAGE_CHANGE_EVENT.event();
    }

    @Override
    public String toString() {
        return "DatalakeImageChangeEvent{" +
                "imageId='" + imageId + '\'' +
                "} " + super.toString();
    }
}
