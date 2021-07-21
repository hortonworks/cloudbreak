package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterUpgradeValidationEvent extends StackEvent {

    private final String imageId;

    private final boolean lockComponents;

    public ClusterUpgradeValidationEvent(String selector, Long resourceId, String imageId, boolean lockComponents) {
        super(selector, resourceId);
        this.imageId = imageId;
        this.lockComponents = lockComponents;
    }

    public ClusterUpgradeValidationEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String imageId) {
        super(selector, resourceId, accepted);
        this.imageId = imageId;
        this.lockComponents = false;
    }

    public ClusterUpgradeValidationEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String imageId, boolean lockComponents) {
        super(selector, resourceId, accepted);
        this.imageId = imageId;
        this.lockComponents = lockComponents;
    }

    public String getImageId() {
        return imageId;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }
}
