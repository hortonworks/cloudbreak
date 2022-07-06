package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class DistroXUpgradeTriggerEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    private final boolean replaceVms;

    private final boolean lockComponents;

    private final String triggeredStackVariant;

    @JsonCreator
    public DistroXUpgradeTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto,
            @JsonProperty("replaceVms") boolean replaceVms,
            @JsonProperty("lockComponents") boolean lockComponents,
            @JsonProperty("triggeredStackVariant") String triggeredStackVariant) {
        super(selector, stackId);
        this.imageChangeDto = imageChangeDto;
        this.replaceVms = replaceVms;
        this.lockComponents = lockComponents;
        this.triggeredStackVariant = triggeredStackVariant;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    public boolean isReplaceVms() {
        return replaceVms;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public String getTriggeredStackVariant() {
        return triggeredStackVariant;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DistroXUpgradeTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageChangeDto=" + imageChangeDto)
                .add("replaceVms=" + replaceVms)
                .add("lockComponents=" + lockComponents)
                .toString();
    }
}
