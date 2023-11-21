package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class UpgradePreparationChainTriggerEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    @JsonCreator
    public UpgradePreparationChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto) {
        super(selector, stackId);
        this.imageChangeDto = imageChangeDto;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpgradePreparationChainTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageChangeDto=" + imageChangeDto)
                .add(super.toString())
                .toString();
    }
}
