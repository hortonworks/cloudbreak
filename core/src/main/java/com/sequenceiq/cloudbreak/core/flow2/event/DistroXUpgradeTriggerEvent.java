package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.StringJoiner;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class DistroXUpgradeTriggerEvent extends StackEvent {

    private final ImageChangeDto imageChangeDto;

    private final boolean replaceVms;

    private final boolean lockComponents;

    public DistroXUpgradeTriggerEvent(String selector, Long stackId, ImageChangeDto imageChangeDto, boolean replaceVms, boolean lockComponents) {
        super(selector, stackId);
        this.imageChangeDto = imageChangeDto;
        this.replaceVms = replaceVms;
        this.lockComponents = lockComponents;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", DistroXUpgradeTriggerEvent.class.getSimpleName() + "[", "]")
                .add("imageChangeDto=" + imageChangeDto)
                .add("replaceVms=" + replaceVms)
                .add("lockComponents=" + lockComponents)
                .toString();
    }
}
