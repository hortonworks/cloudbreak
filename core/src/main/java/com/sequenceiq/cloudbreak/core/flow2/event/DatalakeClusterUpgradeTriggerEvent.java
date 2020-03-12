package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

public class DatalakeClusterUpgradeTriggerEvent extends StackEvent {

    private final StatedImage targetImage;

    public DatalakeClusterUpgradeTriggerEvent(String selector, Long stackId, StatedImage targetImage) {
        super(selector, stackId);
        this.targetImage = targetImage;
    }

    public StatedImage getTargetImage() {
        return targetImage;
    }

}
