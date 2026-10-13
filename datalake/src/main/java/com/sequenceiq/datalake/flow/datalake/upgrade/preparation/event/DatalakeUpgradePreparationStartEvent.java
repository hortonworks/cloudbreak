package com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradePreparationStartEvent extends SdxEvent {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ClouderaManagerV4Request components;

    private final String imageId;

    @JsonCreator
    public DatalakeUpgradePreparationStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("components") ClouderaManagerV4Request components) {
        super(selector, sdxId, userId);
        this.imageId = imageId;
        this.components = components;
    }

    public ClouderaManagerV4Request getComponents() {
        return components;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeUpgradePreparationStartEvent.class, other,
                event -> Objects.equals(imageId, event.imageId)
                        && Objects.equals(components, event.components));
    }

    @Override
    public String toString() {
        return "DatalakeUpgradePreparationStartEvent{" +
                "imageId='" + imageId + '\'' +
                ", components=" + components +
                "} " + super.toString();
    }
}
