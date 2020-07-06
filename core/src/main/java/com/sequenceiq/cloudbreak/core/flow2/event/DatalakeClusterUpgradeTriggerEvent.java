package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

import reactor.rx.Promise;

public class DatalakeClusterUpgradeTriggerEvent extends StackEvent {
    private final StatedImage currentImage;

    private final StatedImage targetImage;

    public DatalakeClusterUpgradeTriggerEvent(String selector, Long stackId, StatedImage currentImage, StatedImage targetImage) {
        super(selector, stackId);
        this.currentImage = currentImage;
        this.targetImage = targetImage;
    }

    public DatalakeClusterUpgradeTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted, StatedImage currentImage, StatedImage targetImage) {
        super(event, resourceId, accepted);
        this.currentImage = currentImage;
        this.targetImage = targetImage;
    }

    public StatedImage getCurrentImage() {
        return currentImage;
    }

    public StatedImage getTargetImage() {
        return targetImage;
    }
}
