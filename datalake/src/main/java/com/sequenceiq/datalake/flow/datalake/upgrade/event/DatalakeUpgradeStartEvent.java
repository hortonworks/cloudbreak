package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeStartEvent extends SdxEvent {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ClouderaManagerV4Request components;

    private final String imageId;

    private final boolean replaceVms;

    private final boolean rollingUpgradeEnabled;

    private final boolean keepVariant;

    //CHECKSTYLE:OFF
    @JsonCreator
    public DatalakeUpgradeStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("replaceVms") boolean replaceVms,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("keepVariant") boolean keepVariant,
            @JsonProperty("components") ClouderaManagerV4Request components) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
        this.replaceVms = replaceVms;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.keepVariant = keepVariant;
        this.components = components;
    }
    //CHECKSTYLE:ON

    public ClouderaManagerV4Request getComponents() {
        return components;
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
                        && rollingUpgradeEnabled == event.rollingUpgradeEnabled
                        && Objects.equals(components, event.components));
    }

    @Override
    public String toString() {
        return "DatalakeUpgradeStartEvent{" +
                "imageId='" + imageId + '\'' +
                ", replaceVms=" + replaceVms +
                ", rollingUpgradeEnabled=" + rollingUpgradeEnabled +
                ", keepVariant=" + keepVariant +
                ", components=" + components +
                "} " + super.toString();
    }
}
