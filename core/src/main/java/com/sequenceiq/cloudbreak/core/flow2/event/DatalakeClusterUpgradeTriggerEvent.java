package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

import reactor.rx.Promise;

public class DatalakeClusterUpgradeTriggerEvent extends StackEvent {

    private final StatedImage targetImage;

    public DatalakeClusterUpgradeTriggerEvent(String selector, Long stackId, StatedImage targetImage) {
        super(selector, stackId);
        this.targetImage = targetImage;
    }

    public DatalakeClusterUpgradeTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted, StatedImage targetImage) {
        super(event, resourceId, accepted);
        this.targetImage = targetImage;
    }

    public StatedImage getTargetImage() {
        return targetImage;
    }

}
