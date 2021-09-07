package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterUpgradeTriggerEvent extends StackEvent {

    private final String imageId;

    public ClusterUpgradeTriggerEvent(String selector, Long stackId, String imageId) {
        super(selector, stackId);
        this.imageId = imageId;
    }

    public ClusterUpgradeTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted, String imageId) {
        super(event, resourceId, accepted);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ClusterUpgradeTriggerEvent.class, other,
                event -> Objects.equals(imageId, event.imageId));
    }
}
