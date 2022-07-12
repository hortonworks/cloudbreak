package com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradePreparationStartEvent extends SdxEvent {

    private final String imageId;

    @JsonCreator
    public DatalakeUpgradePreparationStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeUpgradePreparationStartEvent.class, other,
                event -> Objects.equals(imageId, event.imageId));
    }

    @Override
    public String toString() {
        return "DatalakeUpgradePreparationStartEvent{" +
                "imageId='" + imageId + '\'' +
                "} " + super.toString();
    }
}
