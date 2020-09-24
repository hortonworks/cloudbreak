package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class DatalakeClusterUpgradeTriggerEvent extends StackEvent {

    private final String imageId;

    public DatalakeClusterUpgradeTriggerEvent(String selector, Long stackId, String imageId) {
        super(selector, stackId);
        this.imageId = imageId;
    }

    public DatalakeClusterUpgradeTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted, String imageId) {
        super(event, resourceId, accepted);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }
}
