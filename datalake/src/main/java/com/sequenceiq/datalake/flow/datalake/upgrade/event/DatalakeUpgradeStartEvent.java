package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeStartEvent extends SdxEvent {

    private final String imageId;

    private final boolean replaceVms;

    private final boolean rollingUpgradeEnabled;

    private final boolean keepVariant;

    @JsonCreator
    public DatalakeUpgradeStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("replaceVms") boolean replaceVms,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("keepVariant") boolean keepVariant) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
        this.replaceVms = replaceVms;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.keepVariant = keepVariant;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isReplaceVms() {
        return replaceVms;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    public boolean isKeepVariant() {
        return keepVariant;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeUpgradeStartEvent.class, other,
                event -> Objects.equals(imageId, event.imageId)
                        && replaceVms == event.replaceVms
                        && rollingUpgradeEnabled == event.rollingUpgradeEnabled);
    }

    @Override
    public String toString() {
        return "DatalakeUpgradeStartEvent{" +
                "imageId='" + imageId + '\'' +
                ", replaceVms=" + replaceVms +
                ", rollingUpgradeEnabled=" + rollingUpgradeEnabled +
                "} " + super.toString();
    }
}
