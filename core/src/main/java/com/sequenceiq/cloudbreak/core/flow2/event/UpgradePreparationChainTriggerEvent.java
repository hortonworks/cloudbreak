package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class UpgradePreparationChainTriggerEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    private final boolean lockComponents;

    @JsonCreator
    public UpgradePreparationChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto,
            @JsonProperty("lockComponents") boolean lockComponents) {
        super(selector, stackId);
        this.imageChangeDto = imageChangeDto;
        this.lockComponents = lockComponents;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpgradePreparationChainTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageChangeDto=" + imageChangeDto)
                .add("lockComponents=" + lockComponents)
                .add(super.toString())
                .toString();
    }
}
