package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class UpgradePreparationChainTriggerEvent extends StackEvent {

    private ImageChangeDto imageChangeDto;

    private final String runtimeVersion;

    @JsonCreator
    public UpgradePreparationChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto,
            @JsonProperty("runtimeVersion") String runtimeVersion) {
        super(selector, stackId);
        this.imageChangeDto = imageChangeDto;
        this.runtimeVersion = runtimeVersion;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    public void setImageChangeDto(ImageChangeDto imageChangeDto) {
        this.imageChangeDto = imageChangeDto;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    @Override
    public String toString() {
        return "UpgradePreparationChainTriggerEvent{" +
                "imageChangeDto=" + imageChangeDto +
                ", runtimeVersion='" + runtimeVersion + '\'' +
                "} " + super.toString();
    }
}